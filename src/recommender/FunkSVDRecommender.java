package recommender;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * The Funk SVD recommender algorithm, as described by Simon Funk for the Netflix Prize Challenge.
 */
public class FunkSVDRecommender extends BaseRecommender {

	/*********************************************************************************************************/
	/* 										Private data members											 */
	/*********************************************************************************************************/
	private int NUM_FEATURES;
	private int MIN_ITER;
	private double MIN_IMPROVEMENT;
	private double ALPHA;
	private double BETA;
	private double glAverage;
	private HashMap <Integer, Double> U[];
	private HashMap <Integer, Double> V[];
	private HashMap <Integer, Double> userPseudoAvg;
	private HashMap <Integer, Double> itemPseudoAvg;
	private boolean featuresKnown;
	private String userFeaturesFileName;
	private String itemFeaturesFileName;
	
	
	/*********************************************************************************************************/
	/* 											Public methods											 	 */
	/*********************************************************************************************************/	
	
	/**
	 * This constructor should be used when user preferences and item characteristics in terms of features are
	 * not known and require to be learned over the training data set.
	 * 
	 * @param dao The Data Access Object providing access to the training and test data set.
	 * 
	 * @param NUM_FEATURES The number of features to represent the user tastes and movie attributes. 
	 * A value between 30-50 is good enough. Increases this value improves the recommender accuracy
	 * but slows it down.
	 * 
	 * @param MIN_ITER The minimum number of iterations for which each feature should be trained. A value of 
	 * 100-200 is good enough. Increases this value improves the recommender accuracy but slows it down.
	 * 
	 * @param INIT The initial value with which the matrices representing user tastes and movie
	 * attributes are initialized. Typically a value of 0.1 is used.
	 * 
	 * @param MIN_IMPROVEMENT The minimum improvement in RMSE so that next iteration is attempted. Training of 
	 * each feature continues at least MIN_ITER times or while improvement in RMSE is greater than MIN_IMPROVEMENT.
	 * A value of 0.0001 is good enough.
	 * 
	 * @param ALPHA The learning rate. It controls the step size or the amount of feature adjustment at each 
	 * iteration. A value of 0.001 is good enough. A large learning rate will speed up the learning process but
	 * might not converge. A small learning rate will slow down the learning process.
	 *  
	 * @param BETA The regularization parameter. It helps to smooth out the variations in the learning process. 
	 * A value of 0.015 is good enough.
	 * 
	 * @param userFeaturesFile File name where matrix representing user preferences U[NUM_FEATURES x NUM_USERS] 
	 * is to be stored. These features can be used for recommendation later.
	 *  
	 * @param itemFeaturesFile File name where matrix representing item characteristics V[NUM_FEATURES x NUM_ITEMS] 
	 * is to be stored. These features can be used for recommendation later.
	 */
	@SuppressWarnings("unchecked")
	public FunkSVDRecommender (	DAO dao, 
								int NUM_FEATURES, 
								int MIN_ITER, 
								double INIT, 
								double MIN_IMPROVEMENT, 
								double ALPHA, 
								double BETA,
								String userFeaturesFile, 
								String itemFeaturesFile )	
	{
		super(dao);
		
		this.NUM_FEATURES = NUM_FEATURES;
		this.MIN_ITER = MIN_ITER;
		this.MIN_IMPROVEMENT = MIN_IMPROVEMENT;
		this.ALPHA = ALPHA;
		this.BETA = BETA;
		userFeaturesFileName = userFeaturesFile;
		itemFeaturesFileName = itemFeaturesFile;
		
		U = new HashMap[NUM_FEATURES];
		V = new HashMap[NUM_FEATURES];
		featuresKnown = false;
		
		// Initialize U and V matrices to INIT
		for (int f = 0; f < NUM_FEATURES; f++)	{
			U[f] = new HashMap<Integer, Double>();
			for (int user : dao.getTrainUsers())
				U[f].put(user, INIT);
			V[f] = new HashMap<Integer, Double>();
			for (int item : dao.getAllItems())
				V[f].put(item, INIT);
		}
		
		computeAvgs();
	}
	
	/**
	 * This constructor should be used when user preferences and item characteristics are known in terms of features
	 * and predictions need to be generated for users.
	 * 
	 * @param dao The Data Access Object built on top of the training and test data set.
	 * 
	 * @param userFeaturesFile File name where matrix representing user preferences U[NUM_FEATURES x NUM_USERS] 
	 * is stored. These features are used for recommendation.
	 * 
	 * @param itemFeaturesFile File name where matrix representing item characteristics V[NUM_FEATURES x NUM_ITEMS] 
	 * is stored. These features are used for recommendation.
	 * 
	 * @throws FileNotFoundException if the feature files cannot be opened
	 * @throws IOException if any error occurs during reading the files
	 * @throws NoSuchElementException if the feature files have improper format
	 */
	@SuppressWarnings("unchecked")
	public FunkSVDRecommender (	DAO dao, 
								String userFeaturesFile, 
								String itemFeaturesFile ) 
							throws IOException	
	{
		super(dao);
		
		featuresKnown = true;
		
		// Read user features
		BufferedReader br = new BufferedReader(new FileReader(userFeaturesFile));
		StringTokenizer st = new StringTokenizer(br.readLine());
		NUM_FEATURES = Integer.parseInt(st.nextToken());
		int numUsers = Integer.parseInt(st.nextToken());
		int users[] = new int[numUsers];
		st = new StringTokenizer(br.readLine());
		for (int i = 0; i < numUsers; i++)
			users[i] = Integer.parseInt(st.nextToken());
		U = new HashMap[NUM_FEATURES];
		for (int f = 0; f < NUM_FEATURES; f++)	{
			U[f] = new HashMap<Integer, Double>();
			st = new StringTokenizer(br.readLine());
			for (int i = 0; i < numUsers; i++)
				U[f].put(users[i], Double.parseDouble(st.nextToken()));
		}
		br.close();
		
		// Read item features
		br = new BufferedReader(new FileReader(itemFeaturesFile));
		st = new StringTokenizer(br.readLine());
		NUM_FEATURES = Integer.parseInt(st.nextToken());
		int numItems = Integer.parseInt(st.nextToken());		
		int items[] = new int[numItems];
		st = new StringTokenizer(br.readLine());
		for (int i = 0; i < numItems; i++)
			items[i] = Integer.parseInt(st.nextToken());
		V = new HashMap[NUM_FEATURES];
		for (int f = 0; f < NUM_FEATURES; f++)	{
			V[f] = new HashMap<Integer, Double>();
			st = new StringTokenizer(br.readLine());
			for (int i = 0; i < numItems; i++)
				V[f].put(items[i], Double.parseDouble(st.nextToken()));
		}
		br.close();
		
		computeAvgs();
	}
	
	/**
	 * This method overrides the train() method in the BaseRecommender class, learns user preferences and item
	 * characteristics in terms of features and writes out the learned features to userFeaturesFileName and 
	 * itemFeaturesFileName. It can cause an exception if the files cannot be opened for writing.
	 */
	public void train ()	
	{
		// If the features are already known, then there is no need to train.
		if (featuresKnown)
			return;	
		
		double rmse = 2.0, rmse_last = 10.0, sq, R, P, err, cf, mf;
		int numTotalRatings = dao.numTrainRatings();
		
		// learn values for the features
		for (int f = 0; f < NUM_FEATURES; f++)	{			
			for (int step = 0; (step < MIN_ITER) || (rmse <= rmse_last - MIN_IMPROVEMENT); step++)	{
				sq = 0.0;
				rmse_last = rmse;
				
				for (int user : dao.getTrainUsers())	{
					for (int item : dao.getTrainItems(user))	{
						R = dao.getTrainRating(user, item);
						P = dotProduct(user, item);
						err = (R - (glAverage+(glAverage-itemPseudoAvg.get(item))+(glAverage-userPseudoAvg.get(user))) - P);
						sq += err * err;
						
						cf = U[f].get(user);
						mf = V[f].get(item);
						
						U[f].put(user, cf + ALPHA * (err * mf - BETA * cf));
						V[f].put(item, mf + ALPHA * (err * cf - BETA * mf));
					}
				}				
				rmse = Math.sqrt(sq / numTotalRatings);
			}
		}
		
		// Write out learned features to file
		try {
			// write out user preferences
			BufferedWriter wr = new BufferedWriter(new FileWriter(userFeaturesFileName));
			wr.write(String.format("%d %d\n", NUM_FEATURES, U[0].size()));
			for (Entry<Integer, Double> entry : U[0].entrySet())
				wr.write(String.format("%d\t", entry.getKey()));
			wr.write("\b\n");
			for (int f = 0; f < NUM_FEATURES; f++)	{
				for (Entry<Integer, Double> entry : U[f].entrySet())
					wr.write(String.format("%f\t", entry.getValue()));
				wr.write("\b\n");
			}
			wr.flush();
			wr.close();
			
			// write out item characteristics
			wr = new BufferedWriter(new FileWriter(itemFeaturesFileName));
			wr.write(String.format("%d %d\n", NUM_FEATURES, V[0].size()));
			for (Entry<Integer, Double> entry : V[0].entrySet())
				wr.write(String.format("%d\t", entry.getKey()));
			wr.write("\b\n");
			for (int f = 0; f < NUM_FEATURES; f++)	{
				for (Entry<Integer, Double> entry : V[f].entrySet())
					wr.write(String.format("%f\t", entry.getValue()));
				wr.write("\b\n");
			}
			wr.flush();
			wr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		featuresKnown = true;
	}
	
	/**
	 * This method overrides the predict() method in the BaseRecommender class.
	 * @return the predicted rating that @user will assign to @item, or Double.NEGATIVE_INFINITY 
	 * if a prediction cannot be made
	 */
	public double predict (	int user, 
							int item )	
	{
		if (!U[0].containsKey(user) || !V[0].containsKey(item) || !itemPseudoAvg.containsKey(item) || !userPseudoAvg.containsKey(user))
			return Double.NEGATIVE_INFINITY;
		return ceilPrediction((glAverage+(glAverage-itemPseudoAvg.get(item))+(glAverage-userPseudoAvg.get(user))) + dotProduct(user, item));
	}

	
	/*********************************************************************************************************/
	/* 										Private helper methods											 */
	/*********************************************************************************************************/	
	
	/**
	 * Computes pseudo user average rating for each user, pseudo item average rating for each item and global
	 * average rating over all ratings.
	 */
	private void computeAvgs ()	
	{
		// Find PseudoAvg for each user
		userPseudoAvg = new HashMap<Integer, Double>();
		glAverage = 0.0;
		for (int user : dao.getTrainUsers())	{
			int ratingCount = dao.numTrainRatingsForUser(user);
			double ratingSum = dao.getUserMeanRating(user) * ratingCount;
			glAverage += ratingSum;
			userPseudoAvg.put(user, (3.23 * 25 + ratingSum) / (25.0 + ratingCount));
		}
		glAverage /= dao.numTrainRatings();
		
		// Find PseudoAvg for each item
		itemPseudoAvg = new HashMap<Integer, Double>();
		for (int item : dao.getAllItems())	{
			int ratingCount = dao.numTrainRatingsForItem(item);
			double ratingSum = dao.getItemMeanRating(item) * ratingCount;
			itemPseudoAvg.put(item, (3.23 * 25 + ratingSum) / (25.0 + ratingCount));
		}
	}	
	
	/**
	 * @return the dot product over feature vector of @user with feature vector of @item
	 */
	private double dotProduct (	int user, 
								int item )	
	{
		double sum = 1.0;
		for (int f = 0; f < NUM_FEATURES; f++)
			sum += U[f].get(user) * V[f].get(item);
		return sum;
	}	
}
