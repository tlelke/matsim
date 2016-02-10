/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.parking.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunParkingExampleTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	/**
	 * Test method for {@link org.matsim.contrib.parking.run.RunParkingExample#run(org.matsim.core.config.Config)}.
	 */
	@Test
	public final void testRun() {
		Config config = ConfigUtils.loadConfig("src/main/ressources/config.xml");
		config.controler().setOutputDirectory( utils.getOutputDirectory() );
		config.vspExperimental().setWritingOutputEvents(true);
		RunParkingExample.run(config);
		long expected = CRCChecksum.getCRCFromFile( utils.getInputDirectory() + "/output_events.xml.gz" ) ;
		long actual = CRCChecksum.getCRCFromFile( utils.getOutputDirectory() + "/output_events.xml.gz" ) ;
		Assert.assertEquals(expected, actual);
	}

}