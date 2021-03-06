/*
 * Copyright 2014
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.io.conll;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.uima.fit.util.JCasUtil.*;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.TypeCapability;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.io.JCasFileWriter_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArgLink;
import de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.DependencyFlavor;

/**
 * <p>Writes a file in the CoNLL-2009 format.</p>
 * 
 * <ol>
 * <li>ID - <b>(ignored)</b> Token counter, starting at 1 for each new sentence.</li>
 * <li>FORM - <b>(Token)</b> Word form or punctuation symbol.</li>
 * <li>LEMMA - <b>(Lemma)</b> Fine-grained part-of-speech tag, where the tagset depends on the
 * language, or identical to the coarse-grained part-of-speech tag if not available.</li>
 * <li>PLEMMA - <b>(ignored)</b> Automatically predicted lemma of FORM</li>
 * <li>POS - <b>(POS)</b> Fine-grained part-of-speech tag, where the tagset depends on the language,
 * or identical to the coarse-grained part-of-speech tag if not available.</li>
 * <li>PPOS - <b>(ignored)</b> Automatically predicted major POS by a language-specific tagger</li>
 * <li>FEAT - <b>(MorphologicalFeatures)</b> Unordered set of syntactic and/or morphological features (depending
 * on the particular language), separated by a vertical bar (|), or an underscore if not available.</li>
 * <li>PFEAT - <b>(ignored)</b> Automatically predicted morphological features (if applicable)</li>
 * <li>HEAD - <b>(Dependency)</b> Head of the current token, which is either a value of ID or zero
 * ('0'). Note that depending on the original treebank annotation, there may be multiple tokens with
 * an ID of zero.</li>
 * <li>PHEAD - <b>(ignored)</b> Automatically predicted syntactic head</li>
 * <li>DEPREL - <b>(Dependency)</b> Dependency relation to the HEAD. The set of dependency relations
 * depends on the particular language. Note that depending on the original treebank annotation, the
 * dependency relation may be meaningfull or simply 'ROOT'.</li>
 * <li>PDEPREL - <b>(ignored)</b> Automatically predicted dependency relation to PHEAD</li>
 * <li>FILLPRED - <b>(auto-generated)</b> Contains 'Y' for argument-bearing tokens</li>
 * <li>PRED - <b>(SemanticPredicate)</b> (sense) identifier of a semantic 'predicate' coming from a
 * current token</li>
 * <li>APREDs - <b>(SemanticArgument)</b> Columns with argument labels for each semantic predicate
 * (in the ID order)</li>
 * </ol>
 * 
 * <p>Sentences are separated by a blank new line</p>
 * 
 * @see <a href="http://ufal.mff.cuni.cz/conll2009-st/task-description.html">CoNLL 2009 Shared Task:
 *      predict syntactic and semantic dependencies and their labeling</a>
 * @see <a href="http://www.mt-archive.info/CoNLL-2009-Hajic.pdf">The CoNLL-2009 Shared Task:
 *      Syntactic and Semantic Dependencies in Multiple Languages</a>
 * @see <a href="http://www.aclweb.org/anthology/W08-2121.pdf">The CoNLL-2008 Shared Task on Joint
 *      Parsing of Syntactic and Semantic Dependencies</a>
 */
@TypeCapability(inputs = { "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency",
        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemPred",
        "de.tudarmstadt.ukp.dkpro.core.api.semantics.type.SemArg"})
public class Conll2009Writer
    extends JCasFileWriter_ImplBase
{
    private static final String UNUSED = "_";
    private static final int UNUSED_INT = -1;

    /**
     * Name of configuration parameter that contains the character encoding used by the input files.
     */
    public static final String PARAM_ENCODING = ComponentParameters.PARAM_SOURCE_ENCODING;
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = true, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_FILENAME_SUFFIX = "filenameSuffix";
    @ConfigurationParameter(name = PARAM_FILENAME_SUFFIX, mandatory = true, defaultValue = ".conll")
    private String filenameSuffix;

    public static final String PARAM_WRITE_POS = ComponentParameters.PARAM_WRITE_POS;
    @ConfigurationParameter(name = PARAM_WRITE_POS, mandatory = true, defaultValue = "true")
    private boolean writePos;

    public static final String PARAM_WRITE_MORPH = "writeMorph";
    @ConfigurationParameter(name = PARAM_WRITE_MORPH, mandatory = true, defaultValue = "true")
    private boolean writeMorph;

    public static final String PARAM_WRITE_LEMMA = ComponentParameters.PARAM_WRITE_LEMMA;
    @ConfigurationParameter(name = PARAM_WRITE_LEMMA, mandatory = true, defaultValue = "true")
    private boolean writeLemma;

    public static final String PARAM_WRITE_DEPENDENCY = ComponentParameters.PARAM_WRITE_DEPENDENCY;
    @ConfigurationParameter(name = PARAM_WRITE_DEPENDENCY, mandatory = true, defaultValue = "true")
    private boolean writeDependency;

    public static final String PARAM_WRITE_SEMANTIC_PREDICATE = "writeSemanticPredicate";
    @ConfigurationParameter(name = PARAM_WRITE_SEMANTIC_PREDICATE, mandatory = true, defaultValue = "true")
    private boolean writeSemanticPredicate;

    @Override
    public void process(JCas aJCas)
        throws AnalysisEngineProcessException
    {
        PrintWriter out = null;
        try {
            out = new PrintWriter(new OutputStreamWriter(getOutputStream(aJCas, filenameSuffix),
                    encoding));
            convert(aJCas, out);
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        finally {
            closeQuietly(out);
        }
    }

    private void convert(JCas aJCas, PrintWriter aOut)
    {
        Map<Token, Collection<SemPred>> predIdx = indexCovered(aJCas, Token.class, SemPred.class);
        Map<SemArg, Collection<Token>> argIdx = indexCovered(aJCas, SemArg.class, Token.class);
        for (Sentence sentence : select(aJCas, Sentence.class)) {
            HashMap<Token, Row> ctokens = new LinkedHashMap<Token, Row>();

            // Tokens
            List<Token> tokens = selectCovered(Token.class, sentence);
            
            // Check if we should try to include the FEATS in output
            List<MorphologicalFeatures> morphology = selectCovered(MorphologicalFeatures.class,
                    sentence);
            boolean useFeats = tokens.size() == morphology.size();

            List<SemPred> preds = selectCovered(SemPred.class, sentence);
            
            for (int i = 0; i < tokens.size(); i++) {
                Row row = new Row();
                row.id = i+1;
                row.token = tokens.get(i);
                row.args = new SemArgLink[preds.size()];
                if (useFeats) {
                    row.feats = morphology.get(i);
                }
                
                // If there are multiple semantic predicates for the current token, then 
                // we keep only the first
                Collection<SemPred> predsForToken = predIdx.get(row.token);
                if (predsForToken != null && !predsForToken.isEmpty()) {
                    row.pred = predsForToken.iterator().next();
                }
                ctokens.put(row.token, row);
            }

            // Dependencies
            List<Dependency> basicDeps = selectCovered(Dependency.class, sentence).stream()
                    .filter(dep -> dep.getFlavor() == null || DependencyFlavor.BASIC.equals(dep.getFlavor()))
                    .collect(Collectors.toList());
            for (Dependency rel : basicDeps) {
                Row row =  ctokens.get(rel.getDependent());
                if (row.deprel != null) {
                    throw new IllegalStateException("Illegal basic dependency structure - token ["
                            + row.token.getCoveredText()
                            + "] is dependent of more than one dependency.");
                }
                row.deprel = rel;
            }

            // Semantic arguments
            for (int p = 0; p < preds.size(); p++) {
                FSArray args = preds.get(p).getArguments();
                for (SemArgLink arg : select(args, SemArgLink.class)) {
                    for (Token t : argIdx.get(arg.getTarget())) {
                        Row row = ctokens.get(t);
                        row.args[p] = arg;
                    }
                }
            }
            
            // Write sentence in CONLL 2009 format
            for (Row row : ctokens.values()) {
                int id = row.id;
                
                String form = row.token.getCoveredText();
                
                String lemma = UNUSED;
                if (writeLemma && (row.token.getLemma() != null)) {
                    lemma = row.token.getLemma().getValue();
                }
                String plemma = lemma;

                String pos = UNUSED;
                if (writePos && (row.token.getPos() != null)) {
                    POS posAnno = row.token.getPos();
                    pos = posAnno.getPosValue();
                }
                String ppos = pos;

                String feat = UNUSED;
                if (writeMorph && (row.feats != null)) {
                    feat = row.feats.getValue();
                }
                String pfeat = feat;
                
                int headId = UNUSED_INT;
                String deprel = UNUSED;
                if (writeDependency && (row.deprel != null)) {
                    deprel = row.deprel.getDependencyType();
                    headId = ctokens.get(row.deprel.getGovernor()).id;
                    if (headId == row.id) {
                        // ROOT dependencies may be modeled as a loop, ignore these.
                        headId = 0;
                    }
                }

                String head = UNUSED;
                if (headId != UNUSED_INT) {
                    head = Integer.toString(headId);
                }

                String phead = head;
                String pdeprel = deprel;
                
                String fillpred = UNUSED;
                String pred = UNUSED;
                StringBuilder apreds = new StringBuilder();
                if (writeSemanticPredicate) {
                    if (row.pred != null) {
                        fillpred = "Y";
                        pred = row.pred.getCategory();
                    }
                    
                    for (SemArgLink arg : row.args) {
                        if (apreds.length() > 0) {
                            apreds.append('\t');
                        }
                        apreds.append(arg != null ? arg.getRole() : UNUSED);
                    }
                }

                aOut.printf("%d\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n", id, form,
                        lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred,
                        pred, apreds);
            }

            aOut.println();
        }
    }

    private static final class Row
    {
        int id;
        Token token;
        MorphologicalFeatures feats;
        Dependency deprel;
        SemPred pred;
        SemArgLink[] args; // These are the arguments roles for the current token!
    }
}
