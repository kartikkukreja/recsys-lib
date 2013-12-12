package recommender;

import java.io.*;

/**
 * It is the base class that each recommender class must extend. It provides implementation for 
 * evaluating a given recommender algorithm on the test set. By itself, it returns a user's mean
 * rating as a prediction for any item for that user.
 */
public class BaseRecommender {
	
	/*********************************************************************************************************/
	/* 										Inheritable members											 	 */
	/*********************************************************************************************************/
	protected DAO dao;
	
	/**
	 * @param prediction a double value representing a predicted rating
	 * @return prediction clipped to the interval [1.0, 5.0]
	 */
	protected double ceilPrediction ( double prediction )	
	{
		if (prediction > 5.0)
			prediction = 5.0;
		else if (prediction < 1.0)
			prediction = 1.0;
		return prediction;
	}
	
	
	/*********************************************************************************************************/
	/* 											Public methods											 	 */
	/*********************************************************************************************************/	
	
	/**
	 * @param dao The Data Access Object providing access to the training and test data set.
	 */
	public BaseRecommender ( DAO dao )	
	{
		this.dao = dao;
	}
	
	/**
	 * Each inheriting recommender algorithm will override this method to learn from the training dataset.
	 */
	public void train ()	{	}
	
	/**
	 * Each inheriting recommender algorithm will override this method.
	 * @return the predicted rating that @user will assign to @item, which in case of BaseRecommender is the
	 * mean rating of @user.
	 */
	public double predict (	int user, 
							int item )	
	{
		return dao.getUserMeanRating(user);
	}
	
	/**
	 * Evaluate the performance of the recommender over the test data set and compute performance statistics.
	 * 
	 * @param predictionFileName The output file where predicted outputs for test data set will be stored as
	 * [user item actual_rating predicted_rating] tuples
	 * 
	 * @throws IOException if @predictionFileName cannot be opened for writing or written to.
	 */
	public void evaluate ( String predictionFileName ) 
				throws IOException	
	{
		double MAE = 0.0, RMSE = 0.0;
		int countRatings = 0, countTotal = 0;
				
		BufferedWriter wr = new BufferedWriter(new FileWriter(predictionFileName));
		for (Integer user : dao.getTestUsers())	{
			for (Integer item : dao.getTestItems(user))	{
				countTotal++;
				double P = predict(user, item);
				// A prediction of -INF is used to indicate that the prediction cannot be made
				if (P == Double.NEGATIVE_INFINITY)
					continue;
				countRatings++;
				double R = dao.getTestRating(user, item);
				double tmp = P - R;
				MAE += Math.abs(tmp);
				RMSE += tmp * tmp;
				wr.write(String.format("%d %d %f %f\n", user, item, R, P));
			}
		}
		wr.flush();
		wr.close();

		System.out.println("Recommender evaluation on test data...");
		if (countRatings == 0)
			System.out.println("Coverage : 0%");
		else {
			MAE /= countRatings;
			RMSE = Math.sqrt(RMSE / countRatings);
			System.out.printf("Coverage %f%%\n", (countRatings * 100.0) / countTotal);
			System.out.printf("MAE : %f\n", MAE);
			System.out.printf("RMSE : %f\n", RMSE);
		}		
	}
}
