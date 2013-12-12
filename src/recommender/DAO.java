package recommender;

import java.util.*;
import java.io.*;

/**
 * Class to represent and provide access to training and test data set
 */
public class DAO {
	
	/*********************************************************************************************************/
	/* 										Private data members											 */
	/*********************************************************************************************************/
	private HashMap<Integer, HashMap<Integer, Double>> trainData; 
	private HashMap<Integer, HashMap<Integer, Double>> testData;
	private HashMap<Integer, Double> userMeanRatings;
	private HashMap<Integer, Double> itemMeanRatings;
	private HashMap<Integer, Integer> countItemRatings;
    private HashMap<Integer, HashMap<Integer, Double>> transposed_trainData; 
    private int numOfUsers;
    private int numOfItems;
    private int numOfRatings;
    
    
    /*********************************************************************************************************/
	/* 											Public methods											 	 */
	/*********************************************************************************************************/	

    /**
     * Create a DAO object on top of the training and test data sets.
     * @param trainFileName The file containing [user, item, rating] training tuples
     * @param testFileName The file containing [user, item, rating] test tuples
     * @param delim Deliminator(s) that separate(s) user, item and rating
     * @throws IOException if training or test file could not be opened for reading or read from
     */
	public DAO (String trainFileName, 
				String testFileName, 
				String delim) 
						throws IOException	
	{
		// allocate memory for data members
		trainData = new HashMap<Integer, HashMap<Integer, Double>>(); 
		testData = new HashMap<Integer, HashMap<Integer, Double>>();
		userMeanRatings = new HashMap<Integer, Double>();
		itemMeanRatings = new HashMap<Integer, Double>();
		countItemRatings = new HashMap<Integer, Integer>();
		transposed_trainData = new HashMap<Integer, HashMap<Integer, Double>>();
		
		// read training data
		BufferedReader trainFile = new BufferedReader(new FileReader(trainFileName));
		String line = null;
		
		numOfRatings = 0;
		while ((line = trainFile.readLine()) != null)	{
			StringTokenizer st = new StringTokenizer(line, delim);
			int user = Integer.parseInt(st.nextToken());
			int item = Integer.parseInt(st.nextToken());
			double rating = Double.parseDouble(st.nextToken());
			numOfRatings++;
			
			if (trainData.containsKey(user))
				trainData.get(user).put(item, rating);
			else	{
				HashMap<Integer, Double> userRatings = new HashMap<Integer, Double>();
				userRatings.put(item, rating);
				trainData.put(user, userRatings);
			}
			
			if(transposed_trainData.containsKey(item))
                transposed_trainData.get(item).put(user, rating);
            else    {
                HashMap<Integer, Double> userRatings = new HashMap<Integer, Double>();
                userRatings.put(user, rating);
                transposed_trainData.put(item, userRatings);
            }
			
			if (userMeanRatings.containsKey(user))
				userMeanRatings.put(user, userMeanRatings.get(user) + rating);
			else
				userMeanRatings.put(user, rating);
			if (itemMeanRatings.containsKey(item))	{
				itemMeanRatings.put(item, itemMeanRatings.get(item) + rating);
				countItemRatings.put(item, countItemRatings.get(item) + 1);
			} else	{
				itemMeanRatings.put(item, rating);
				countItemRatings.put(item, 1);
			}
		}
		trainFile.close();
		
		numOfUsers = trainData.keySet().size();
		numOfItems = transposed_trainData.keySet().size();
		
		// compute mean user and item ratings
		for (Integer user : userMeanRatings.keySet())
			userMeanRatings.put(user, userMeanRatings.get(user) / trainData.get(user).size());
		
		for (Integer item : itemMeanRatings.keySet())
			itemMeanRatings.put(item, itemMeanRatings.get(item) / countItemRatings.get(item));
		
		// read test data
		BufferedReader testFile = new BufferedReader(new FileReader(testFileName));
		line = null;
		
		while ((line = testFile.readLine()) != null)	{
			StringTokenizer st = new StringTokenizer(line, delim);
			int user = Integer.parseInt(st.nextToken());
			int item = Integer.parseInt(st.nextToken());
			double rating = Double.parseDouble(st.nextToken());
			
			if (testData.containsKey(user))
				testData.get(user).put(item, rating);
			else	{
				HashMap<Integer, Double> userRatings = new HashMap<Integer, Double>();
				userRatings.put(item, rating);
				testData.put(user, userRatings);
			}
		}
		testFile.close();
	}

	/**
	 * @return the rating associated with (@user, @item) in the training data set. If @user has not rated @item,
	 * 0 is returned
	 * 
	 * @throws IllegalArgumentException if @user is not present in the training data
	 */
	public double getTrainRating (	int user, 
									int item )	
	{
		if (trainData.containsKey(user))	{
			HashMap<Integer, Double> userRatings = trainData.get(user);
			if (userRatings.containsKey(item))
				return userRatings.get(item);
			else
				return 0;
		} else
			throw new IllegalArgumentException(String.format("Training data does not contain any rating for user %d.", user));
	}
	
	/**
	 * @return the rating associated with (@user, @item) in the test data set
	 * 
	 * @throws IllegalArgumentException if @user is not present in the test data set or (@user, @item) pair does not
	 * exist in the test data set
	 */
	public double getTestRating (	int user, 
									int item )	
	{
		if (testData.containsKey(user))	{
			HashMap<Integer, Double> userRatings = testData.get(user);
			if (userRatings.containsKey(item))
				return userRatings.get(item);
			else
				throw new IllegalArgumentException(String.format("Test data does not contain rating for item %d for user %d.", item, user));
		} else
			throw new IllegalArgumentException(String.format("Test data does not contain any rating for user %d.", user));
	}

	/**
	 * @return the mean rating for @user in the training data set
	 */
	public double getUserMeanRating ( int user )	
	{
		if (userMeanRatings.containsKey(user))
			return userMeanRatings.get(user);
		else
			return 3.5;
	}
	
	/**
	 * @return the mean rating for @item in the training data set
	 */
	public double getItemMeanRating ( int item )	
	{
		if (itemMeanRatings.containsKey(item))
			return itemMeanRatings.get(item);
		else
			return 3.5;
	}
	
	/**
	 * @return an iterator over all users in the training data set
	 */
	public Iterable<Integer> getTrainUsers ()	
	{
		return trainData.keySet();
	}
	
	/**
	 * @return an iterator over all users in the test data set
	 */
	public Iterable<Integer> getTestUsers ()	
	{
		return testData.keySet();
	}
	
	/**
	 * @return an iterator over all items rated by @user in the training data set
	 */
	public Iterable<Integer> getTrainItems ( int user )	
	{
		return trainData.get(user).keySet();
	}
	
	/**
	 * @return an iterator over all items rated by @user in the test data set
	 */
	public Iterable<Integer> getTestItems ( int user )	
	{
		return testData.get(user).keySet();
	}
	
	/**
	 * @return an iterator over all items in the transposed training data set
	 */
	public Iterable<Integer> getTransposedTrainItems ()
	{
        return transposed_trainData.keySet();
    }
	
	/**
	 * @return an iterator over all users who have rated @item in the transposed training data set
	 */
	public Iterable<Integer> getTransposedTrainUsers ( int item )	
	{
		return transposed_trainData.get(item).keySet();
	}

	/**
	 * @return an iterator over all items in the training data set
	 */
	public Iterable<Integer> getAllItems ()
	{
		return transposed_trainData.keySet();
	}

	/**
	 * @return true if (@item, @user) has a rating in the transposed training data set, false otherwise
	 */
	public boolean containsTransposedTrainRating (	int item, 
													int user )	
	{
		return transposed_trainData.containsKey(item) && transposed_trainData.get(item).containsKey(user);
	}
	
	/**
	 * @return the rating associated with (@item, @user) in the transposed training data set
	 */
	public double getTransposedTrainRating (int item, 
											int user )	
	{
		return transposed_trainData.get(item).get(user);
	}

	/**
	 * @return true if @user exists in the training data set
	 */
	public boolean containsTrainUser ( int user )	
	{
		return trainData.containsKey(user);
	}
		
	/**
	 * @return true if (@user, @item) has a rating in the training data set, false otherwise
	 */
	public boolean containsTrainRating (int user, 
										int item )	
	{
		return trainData.containsKey(user) && trainData.get(user).containsKey(item);
	}

	/**
	 * Normalize the training data by subtracting user mean rating for each rating in the data set.
	 */
	public void userMeanNormalize ()	
	{
		for (Integer user : trainData.keySet())	{
			double userMean = userMeanRatings.get(user);
			for (Integer item : trainData.get(user).keySet())
				trainData.get(user).put(item, trainData.get(user).get(item) - userMean);
		}			
	}
	
	/**
	 * Normalize the transposed training data by subtracting item mean rating for each rating in the data set.
	 */
	public void itemMeanNormalize ()
	{
		for (Integer item : transposed_trainData.keySet())
			for (Integer user : transposed_trainData.get(item).keySet())
				transposed_trainData.get(item).put(user, transposed_trainData.get(item).get(user) - itemMeanRatings.get(item));
	}

	/**
	 * Denormalize the training data by adding user mean rating for each rating in the data set.
	 */
	public void userMeanDeNormalize ()
	{
		for (Integer user : trainData.keySet())	{
			double userMean = userMeanRatings.get(user);
			for (Integer item : trainData.get(user).keySet())
				trainData.get(user).put(item, trainData.get(user).get(item) + userMean);
		}
	}
	
	/**
	 * Denormalize the transposed training data by adding item mean rating for each rating in the data set.
	 */
	public void itemMeanDeNormalize ()
	{
		for (Integer item : transposed_trainData.keySet())
			for (Integer user : transposed_trainData.get(item).keySet())
				transposed_trainData.get(item).put(user, transposed_trainData.get(item).get(user) + itemMeanRatings.get(item));
	}
	
	/**
	 * @return the total number of users in the training data set
	 */
	public int numTrainUsers ()
	{
		return numOfUsers;
	}
	
	/**
	 * @return the total number of items in the training data set
	 */
	public int numTrainItems ()
	{
		return numOfItems;
	}

	/**
	 * @return the total number of ratings in the training data set
	 */
	public int numTrainRatings ()
	{
		return numOfRatings;
	}
	
	/**
	 * @return the number of items rated by @user in the training data set
	 */
	public int numTrainRatingsForUser ( int user )	
	{
		return trainData.get(user).size();
	}
	
	/**
	 * @return the number of users who have rated @item in the training data set
	 */
	public int numTrainRatingsForItem ( int item )	
	{
		return transposed_trainData.get(item).size();
	}
	
}
