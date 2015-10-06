/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.jbischoff.av.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.jbischoff.taxi.evaluation.TaxiCustomerWaitTimeAnalyser;
import playground.jbischoff.taxi.evaluation.TravelDistanceTimeEvaluator;

/**
 * @author  jbischoff
 *
 */
public class EventsAnalyser {

	public static void main(String[] args) {
		String pre = "C:/Users/Joschka/Documents/shared-svn/projects/audi_av/scenario/";
		String inputFile = pre+"events.out.xml";
		EventsManager events = EventsUtils.createEventsManager();
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(pre+"networkc.xml.gz");
		TravelDistanceTimeEvaluator tdtc = new TravelDistanceTimeEvaluator(scenario.getNetwork(), 24*3600);
		TaxiCustomerWaitTimeAnalyser twc = new TaxiCustomerWaitTimeAnalyser(scenario, Double.MAX_VALUE);
		events.addHandler(twc);
		events.addHandler(tdtc);
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		twc.printTaxiCustomerWaitStatistics();
		twc.writeCustomerWaitStats(pre+"waitstats.txt");
		tdtc.printTravelDistanceStatistics();
		tdtc.writeTravelDistanceStatsToFiles(pre+"distanceStats");
				
		
	}

}