/*
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.dkpro.core.api.segmentation.type;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.fit.factory.JCasBuilder;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Before;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Compound.CompoundSplitLevel;

public class CompoundTest
{

	private Compound compound;

	@Before
	public void setUpCompound()
			throws UIMAException
			{
		final JCas jcas = JCasFactory.createJCas();
		final JCasBuilder jcasBuilder = new JCasBuilder(jcas);
		final int beginPosition = jcasBuilder.getPosition();
		final CompoundPart getrank = jcasBuilder.add("getränk", CompoundPart.class);
		final int secondPosition = jcasBuilder.getPosition();
		final CompoundPart auto = jcasBuilder.add("auto", CompoundPart.class);
		final CompoundPart mat = jcasBuilder.add("mat", CompoundPart.class);
		final CompoundPart automat = new CompoundPart(jcas, secondPosition,
				jcasBuilder.getPosition());
		final List<Split> splits = new ArrayList<Split>();
		splits.add(auto);
		splits.add(mat);
		automat.setSplits(FSCollectionFactory.createFSArray(jcas, splits));
		automat.addToIndexes();
		compound = new Compound(jcas, beginPosition, jcasBuilder.getPosition());
		splits.clear();
		splits.add(getrank);
		splits.add(automat);
		compound.setSplits(FSCollectionFactory.createFSArray(jcas, splits));
		compound.addToIndexes();
		jcasBuilder.close();

			}

	@Test
	public void testAll()
			throws UIMAException
			{

		final String[] splitsList = new String[] { "getränk", "automat", "auto", "mat" };
		assertThat(coveredTextArrayFromAnnotations(compound.getSplitsWithoutMorpheme(CompoundSplitLevel.ALL)),
				is(splitsList));

			}

	@Test
	public void testLowest()
			throws UIMAException
			{

		final String[] splitsList = new String[] { "getränk", "auto", "mat" };
		assertThat(coveredTextArrayFromAnnotations(compound.getSplitsWithoutMorpheme(CompoundSplitLevel.LOWEST)),
				is(splitsList));

			}

	@Test
	public void testHighest()
			throws UIMAException
			{

		final String[] splitsList = new String[] { "getränk", "automat" };
		assertThat(coveredTextArrayFromAnnotations(compound.getSplitsWithoutMorpheme(CompoundSplitLevel.HIGHEST)),
				is(splitsList));

			}

	@Test
	public void testNone()
			throws UIMAException
			{

		final String[] splitsList = new String[] { };
		assertThat(coveredTextArrayFromAnnotations(compound.getSplitsWithoutMorpheme(CompoundSplitLevel.NONE)),
				is(splitsList));

			}

	public <T extends Annotation> String[] coveredTextArrayFromAnnotations(final T[] annotations)
	{
		final List<String> list = new ArrayList<String>();
		for (T annotation : annotations) {
			list.add(annotation.getCoveredText());
		}
		return list.toArray(new String[list.size()]);
	}

}
