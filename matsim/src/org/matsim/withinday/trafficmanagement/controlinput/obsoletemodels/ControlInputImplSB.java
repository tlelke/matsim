/* *********************************************************************** *
 * project: org.matsim.*
 * ControlInputImplSB.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.withinday.trafficmanagement.controlinput.obsoletemodels;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.mobsim.queuesim.QueueLink;
import org.matsim.mobsim.queuesim.SimulationTimer;
import org.matsim.network.Link;
import org.matsim.population.Route;
import org.matsim.withinday.trafficmanagement.AbstractControlInputImpl;
import org.matsim.withinday.trafficmanagement.Accident;
import org.matsim.withinday.trafficmanagement.ControlInput;
import org.matsim.withinday.trafficmanagement.controlinput.ControlInputWriter;

/*
 * FIXME [kn] Because this class was build to replace NashWriter, it inherits a serious flaw:
 * This class takes args of type Route in ctor, and returns arguments of
 * type route at getRoute, but these routes are of different type (one with FakeLink, the other
 * with behavioral links).
 */

/* Does the same as ControlInputImpl1.java, but also:
Predicts the travel time on the two routes, WITHOUT base traffic flows.
Travel time on the congested route is calculated by dividing the route
in two parts, one before and one after the accident location.
Model checks if there will be a queue or not and calculates the travel times according to
the outflow through the bottleneck and the free flow velocity.
*/

public class ControlInputImplSB extends AbstractControlInputImpl
implements EventHandlerLinkLeaveI, EventHandlerLinkEnterI,
EventHandlerAgentDepartureI, EventHandlerAgentArrivalI, ControlInput{

	private static final Logger log = Logger.getLogger(ControlInputImplSB.class);

	double predTTRoute1;

	double predTTRoute2;

	private ControlInputWriter writer;

	private List<Accident> accidents;

	public ControlInputImplSB() {
		super();
		this.writer = new ControlInputWriter();
	}

	@Override
	public void init() {
		super.init();
		this.writer.open();
	}

	@Override
	public void handleEvent(final EventLinkEnter event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final EventLinkLeave event) {
			super.handleEvent(event);
	}

	public void reset(int iteration) {
		//nothing need to be done here anymore cause everything is done in the finishIteration().
	}

	public void finishIteration() {
		BufferedWriter w1 = null;
		BufferedWriter w2 = null;
		try{
			w1 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredMainRoute.txt"));
			w2 = new BufferedWriter(new FileWriter("../studies/arvidDaniel/output/ttMeasuredAlternativeRoute.txt"));
		}catch(IOException e){
			e.printStackTrace();
		}

		Iterator<Double> it1 = this.ttMeasuredMainRoute.iterator();
		try{
			while(it1.hasNext()){
				double measuredTimeMainRoute = it1.next();
				w1.write(Double.toString(measuredTimeMainRoute));
				w1.write("\n");
				w1.flush();
			}
		}catch (IOException e){
			e.printStackTrace();
		}

		Iterator<Double> it2 = this.ttMeasuredAlternativeRoute.iterator();
		try{
			while(it2.hasNext()){
				double measuredTimeAlternativeRoute = it2.next();
				w2.write(Double.toString(measuredTimeAlternativeRoute));
				w2.write("\n");
				w2.flush();
			}
		}catch (IOException e){
				e.printStackTrace();
		}
		try {
			w1.close();
			w2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.writer.close();
	}

	@Override
	public void handleEvent(final EventAgentDeparture event) {
		super.handleEvent(event);
	}

	@Override
	public void handleEvent(final EventAgentArrival event) {
		super.handleEvent(event);
	}

	// calculates the predictive NashTime with a single-bottle-neck-model.
	public double getPredictedNashTime() {
		if (this.accidents.isEmpty()) {
			throw new UnsupportedOperationException("To use this controler an accident has to be set");
		}
		String accidentLinkId = this.accidents.get(0).getLinkId();


		Link bottleNeckLinkRoute1 = searchAccidentsOnRoutes(accidentLinkId);

//		use first link on alternative route as default
//		Link bottleNeckLinkRoute2 = this.alternativeRoute.getLinkRoute()[0];

//		use natural bottle neck as default link
		Link bottleNeckLinkRoute2 = this.altRouteNaturalBottleNeck;

		this.predTTRoute1 = getPredictedTravelTime(this.mainRoute, bottleNeckLinkRoute1);
		this.predTTRoute2 = getPredictedTravelTime(this.alternativeRoute, bottleNeckLinkRoute2);
		if (log.isTraceEnabled()) {
			log.trace("predicted time route 1: " + this.predTTRoute1);
			log.trace("predicted time route 2: " + this.predTTRoute2);
		}
		return this.predTTRoute1 - this.predTTRoute2;
	}


	private double getPredictedTravelTime(final Route route,
			final Link bottleNeckLink) {
		Link [] routeLinks = route.getLinkRoute();
		double ttFreeSpeedBeforeBottleNeck = 0.0;
		double bottleNeckCapacity = ((QueueLink)bottleNeckLink).getSimulatedFlowCapacity()/SimulationTimer.getSimTickTime();
		double predictedTT;
		// get the array index of the bottleneck link
		int bottleNeckLinkNumber = 0;
		for (int i = 0; i < routeLinks.length; i++) {
			if (bottleNeckLink.equals(routeLinks[i])) {
				bottleNeckLinkNumber = i;
				break;
			}
		}

		// count agents and free speed travel time(really only needed once..  if bottleneck does not move)
		// BEFORE bottleneck
		int agentsBeforeBottleNeck = 0;
		for (int i = 0; i <= bottleNeckLinkNumber; i++) {
			agentsBeforeBottleNeck += this.numberOfAgents.get(routeLinks[i]
					.getId().toString());

			ttFreeSpeedBeforeBottleNeck += this.ttFreeSpeeds.get(routeLinks[i]
					.getId().toString());
		}

		// sum up free speed travel time(really only needed once...) AFTER
		// bottleneck
		double ttFreeSpeedAfterBottleNeck = 0 ;
			for (int i = bottleNeckLinkNumber + 1; i <routeLinks.length ; i++) {
			ttFreeSpeedAfterBottleNeck += this.ttFreeSpeeds.get(routeLinks[i]
					.getId().toString());
			}

		if (agentsBeforeBottleNeck / bottleNeckCapacity > ttFreeSpeedBeforeBottleNeck) {
				predictedTT = (agentsBeforeBottleNeck / bottleNeckCapacity)
					+ ttFreeSpeedAfterBottleNeck;
		} else {
			predictedTT = ttFreeSpeedBeforeBottleNeck
					+ ttFreeSpeedAfterBottleNeck;
				}
		return predictedTT;
	}


	private Link searchAccidentsOnRoutes(final String accidentLinkId) {
		Route r = this.mainRoute;
		for (int j = 0; j < 2; j++) {
			Link[] links = r.getLinkRoute();
			for (int i = 0; i < links.length; i++) {
				if (links[i].getId().toString().equalsIgnoreCase(accidentLinkId)) {
					return links[i];
				}
			}
			r = this.alternativeRoute;
		}
		throw new IllegalArgumentException("The set Accident has to be on one of the routes if using this implementation of ControlInput!");
	}


	// ContolInputI interface methods:

	public double getNashTime() {
		try {
			this.writer.writeAgentsOnLinks(this.numberOfAgents);
			this.writer.writeTravelTimesMainRoute(this.lastTimeMainRoute,
					this.predTTRoute1);
			this.writer.writeTravelTimesAlternativeRoute(this.lastTimeAlternativeRoute,
					this.predTTRoute2);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return getPredictedNashTime();

	}

	public void setAccidents(final List<Accident> accidents) {
		this.accidents = accidents;
	}


}
