package vrp.algorithms.ruinAndRecreate.basics;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;

import vrp.algorithms.ruinAndRecreate.api.TourActivityStatusUpdater;
import vrp.algorithms.ruinAndRecreate.api.TourBuilder;
import vrp.api.Constraints;
import vrp.api.Costs;
import vrp.api.Customer;
import vrp.api.Node;
import vrp.basics.Tour;
import vrp.basics.TourActivity;
import vrp.basics.VrpUtils;

/**
 * 
 * @author stefan schroeder
 *
 */

public class BestTourBuilder implements TourBuilder {
	
	private static Logger logger = Logger.getLogger(BestTourBuilder.class);
	
	public static class TourResult {
		public Tour tour;
		public double totalCosts;
		public double marginalCosts;
		public TourResult(Tour tour, double totalCosts, double marginalCosts) {
			this.tour = tour;
			this.totalCosts = totalCosts;
			this.marginalCosts = marginalCosts;
		}
	}
	
	private Costs costs;
	
	private Constraints constraints = new Constraints(){

		@Override
		public boolean judge(Tour tour) {
			return true;
		}
		
	};
	
	private TourActivityStatusUpdater tourActivityUpdater;
	
	public void setTourActivityStatusUpdater(TourActivityStatusUpdater tourActivityUpdater) {
		this.tourActivityUpdater = tourActivityUpdater;
	}
	
	public void setCosts(Costs costs){
		this.costs = costs;
	}
	
	public void setConstraints(Constraints constraints){
		this.constraints = constraints;
	}
	
	private TourResult buildTour(Tour tour, Customer customer){
		double bestMarginalCost = Double.MAX_VALUE;
		Tour bestTour = null;
		double bestTotalCosts = Double.MAX_VALUE;
		for(int i=1;i<tour.getActivities().size();i++){	
				double marginalCost = getCosts(getActLocation(tour, i-1), customer.getLocation()) + 
					getCosts(customer.getLocation(), getActLocation(tour, i)) - getCosts(getActLocation(tour, i-1), getActLocation(tour, i));
				if(marginalCost < bestMarginalCost){
					Tour newTour = VrpUtils.createEmptyCustomerTour();
					for(TourActivity tA : tour.getActivities()){
						newTour.getActivities().add(VrpUtils.createTourActivity(tA.getCustomer()));
					}
					newTour.getActivities().add(i,VrpUtils.createTourActivity(customer));
					assertCustomerIsOnlyOnceInTour(newTour,customer);
					tourActivityUpdater.update(newTour);
					double totalCosts = newTour.costs.generalizedCosts;
					if(this.constraints.judge(newTour)){
						bestMarginalCost = marginalCost; 
						bestTour = newTour;
						bestTotalCosts = totalCosts;
					}
				}
		}
		if(bestTour != null){
			return new TourResult(bestTour,bestTotalCosts,bestMarginalCost);
		}
		return null;
	}
	
	private void assertCustomerIsOnlyOnceInTour(Tour newTour, Customer customer) {
		if(isDepot(newTour, customer)){
			return;
		}
		Id customerId = customer.getId();
		int count = 0;
		for(TourActivity tA : newTour.getActivities()){
			if(tA.getCustomer().getId().equals(customerId)){
				count++;
			}
		}
		if(count<1 || count>1){
			logger.error(newTour + " this cannot happen");
			System.exit(1);
		}
		
	}

	public Tour addShipmentAndGetTour(Tour tour, Shipment shipment){
		verify();
		TourResult tourTrippel = null;
		if(isDepot(tour,shipment.getFrom())){
			tourTrippel = buildTour(tour, shipment.getTo());
		}
		else if(isDepot(tour,shipment.getTo())){
			tourTrippel = buildTour(tour, shipment.getFrom());
		}
		else{
			tourTrippel = buildTourWithEnRoutePickupAndDelivery(tour,shipment);
		}
		return tourTrippel.tour;
	}
	
	private void verify() {
		if(tourActivityUpdater == null){
			throw new IllegalStateException("tourActivityStatusUpdater is not set. this cannot be.");
		}
		if(costs == null){
			throw new IllegalStateException("costsObj is not set. this cannot be");
		}
		
	}

	private TourResult buildTourWithEnRoutePickupAndDelivery(Tour tour, Shipment shipment) {
		Node fromLocation = shipment.getFrom().getLocation();
		Node toLocation = shipment.getTo().getLocation();
		Double bestMarginalCost = Double.MAX_VALUE;
		Double bestTotalCosts = null;
		Tour bestTour = null;
		for(int i=1;i<tour.getActivities().size();i++){
			double marginalCostComp1 = getCosts(getActLocation(tour, i-1),fromLocation) + getCosts(fromLocation,getActLocation(tour, i)) - getCosts(getActLocation(tour, i-1),getActLocation(tour, i));
			for(int j=i;j<tour.getActivities().size();j++){
				double marginalCost;
				if(i == j){
					marginalCost = getCosts(getActLocation(tour, i-1),fromLocation) + getCosts(fromLocation,toLocation) + getCosts(toLocation,getActLocation(tour, i)) -
						getCosts(getActLocation(tour, i-1),getActLocation(tour, i));
				}
				else{
					double marginalCostComp2 = getCosts(getActLocation(tour, j-1),toLocation) + getCosts(toLocation,getActLocation(tour, j)) - getCosts(getActLocation(tour, j-1),getActLocation(tour, j));
					marginalCost = marginalCostComp1 + marginalCostComp2;
				}
				if(marginalCost < bestMarginalCost){
					Tour newTour = buildTour(tour,shipment,i,j);
					tourActivityUpdater.update(newTour);
					double totCosts = newTour.costs.generalizedCosts;
					if(this.constraints.judge(newTour)){
						bestMarginalCost = marginalCost;
						bestTour = newTour;
						bestTotalCosts = totCosts;
					}
				}
			}
		}
		if(bestTour != null){
			return new TourResult(bestTour,bestTotalCosts,bestMarginalCost);
		}
		return null;
	
	}

	private boolean isDepot(Tour tour, Customer customer) {
		if(customer.getId().equals(getDepot(tour).getId())){
			return true;
		}
		return false;
	}
	
	private Customer getDepot(Tour tour){
		return tour.getActivities().get(0).getCustomer();
	}

	private Tour buildTour(Tour tour, Shipment shipment, int i, int j) {
		Tour newTour = VrpUtils.createEmptyCustomerTour();
		for(TourActivity tA : tour.getActivities()){
			newTour.getActivities().add(VrpUtils.createTourActivity(tA.getCustomer()));
		}
		newTour.getActivities().add(i,VrpUtils.createTourActivity(shipment.getFrom()));
		newTour.getActivities().add(j+1,VrpUtils.createTourActivity(shipment.getTo()));
		return newTour;
	}

	private double getCosts(Node from, Node to) {
		return costs.getCost(from, to);
	}

	private Node getActLocation(Tour tour, int i) {
		return tour.getActivities().get(i).getLocation();
	}


}
