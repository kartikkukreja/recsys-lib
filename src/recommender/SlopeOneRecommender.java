package recommender;

import java.util.HashMap;

/**
 * The Slope One item-based recommender algorithm
 */
public class SlopeOneRecommender extends BaseRecommender {
	
	/*********************************************************************************************************/
	/* 										Private data members											 */
	/*********************************************************************************************************/
	private HashMap<Integer, HashMap<Integer, Double>> itemItemDiff;
	private HashMap<Integer, HashMap<Integer, Integer>> itemItemFreq;
	private HashMap<Integer, HashMap<Integer, Double>> predictions;
	
	
	/*********************************************************************************************************/
	/* 											Public methods											 	 */
	/*********************************************************************************************************/	
	
	/**
	 * @param dao The Data Access Object built on top of the training and test data set.
	 */
	public SlopeOneRecommender (DAO dao)	
	{
		super(dao);
		
		itemItemDiff = new HashMap<Integer, HashMap<Integer, Double>>();
		itemItemFreq = new HashMap<Integer, HashMap<Integer, Integer>>();
		predictions = new HashMap<Integer, HashMap<Integer, Double>>();
	}
	
	/**
	 * This method overrides the train() method in the BaseRecommender class and precomputes predictions
	 * for each missing rating in the training data set.
	 */
	public void train ()	
	{
		for (int user : dao.getTrainUsers())	{
			for (int item : dao.getTrainItems(user))	{
				if (!itemItemDiff.containsKey(item))	{
					itemItemDiff.put(item, new HashMap<Integer, Double>());
					itemItemFreq.put(item, new HashMap<Integer, Integer>());
				}
				
				double rating = dao.getTrainRating(user, item);
				HashMap<Integer, Double> itemDiff = itemItemDiff.get(item);
				HashMap<Integer, Integer> itemFreq = itemItemFreq.get(item);
				
				for (int item2 : dao.getTrainItems(user))	{
					double rating2 = dao.getTrainRating(user, item2);
					if (!itemDiff.containsKey(item2))	{
						itemDiff.put(item2, 0.0);
						itemFreq.put(item2, 0);
					}
					itemDiff.put(item2, itemDiff.get(item2) + rating - rating2);
					itemFreq.put(item2, itemFreq.get(item2) + 1);										
				}
			}
		}	
		
		for (int item : itemItemDiff.keySet())	{
			HashMap<Integer, Double> itemDiff = itemItemDiff.get(item);
			HashMap<Integer, Integer> itemFreq = itemItemFreq.get(item);
			
			for (int item2 : itemDiff.keySet())
				itemDiff.put(item2, itemDiff.get(item2) / itemFreq.get(item2));
		}
		
		for (int user : dao.getTrainUsers())	{
			HashMap<Integer, Double> preds = new HashMap<Integer, Double>();
			HashMap<Integer, Integer> freqs = new HashMap<Integer, Integer>();
			
			for (int item : dao.getTrainItems(user))	{
				double rating = dao.getTrainRating(user, item);
				for (int diffitem : itemItemDiff.keySet())	{
					HashMap<Integer, Double> itemDiff = itemItemDiff.get(diffitem);
					HashMap<Integer, Integer> itemFreq = itemItemFreq.get(diffitem);
					
					if (!itemFreq.containsKey(item))
						continue;
					int freq = itemFreq.get(item);
					
					if (!preds.containsKey(diffitem))	{
						preds.put(diffitem, 0.0);
						freqs.put(diffitem, 0);
					}
					preds.put(diffitem, preds.get(diffitem) + freq * (itemDiff.get(item) + rating));
					freqs.put(diffitem, freqs.get(diffitem) + freq);
				}
			}
			
			for (int item : itemItemDiff.keySet())	{
				if (dao.containsTrainRating(user, item))
					preds.remove(item);
				else if (preds.containsKey(item)) {
					double val = preds.get(item);
					int freq = freqs.get(item);
					if (freq > 0)
						preds.put(item, val / freq);
					else
						preds.remove(item);
				}
			}
			predictions.put(user, preds);
		}		
	}
	
	/**
	 * This method overrides the predict() method in the BaseRecommender class.
	 * @return the predicted rating that @user will assign to @item, or Double.NEGATIVE_INFINITY 
	 * if a prediction cannot be made
	 */
	public double predict(int user, int item)	{
		if (!predictions.containsKey(user) || !predictions.get(user).containsKey(item))
			return Double.NEGATIVE_INFINITY;
		
		return ceilPrediction(predictions.get(user).get(item));
	}
}
