package recommender;

import java.awt.Point;
import java.util.*;

/**
 * The User-User Collaborative Filtering Algorithm using Vector Cosine Similarity
 */
public class UUCollaborativeFiltering extends BaseRecommender {

	/*********************************************************************************************************/
	/* 										Private data members											 */
	/*********************************************************************************************************/
	private HashMap<Integer, ArrayList<Integer>> neighbors;
	private HashMap<Integer, HashMap<Integer, Double>> similarity;
	private int NEIGHBORHOOD_SIZE;
	private int MIN_NEIGHBORS;
	
	
	/*********************************************************************************************************/
	/* 											Public methods											 	 */
	/*********************************************************************************************************/	
	
	/**
	 * @param dao The Data Access Object built on top of the training and test data set.
	 * 
	 * @param NEIGHBORHOOD_SIZE The maximum number of neighbors of each user. We used a value of 378 on
	 * the Movielens 100K dataset. Increasing the neighborhood size increases memory and time requirements
	 * of the algorithm. Too small neighborhood size may limit the coverage of the algorithm. Both too small
	 * or too large neighborhood may increase the error.
	 * 
	 * @param MIN_NEIGHBORS Minimum number of neighbors that a user must have, which have rated a given item,
	 * before we can predict a rating for the item. We used a value of 12 on the Movielens 100K dataset.
	 * Too small a value may increase the error while too large a value may decrease the coverage drastically.
	 */
	public UUCollaborativeFiltering (	DAO dao, 
										int NEIGHBORHOOD_SIZE, 
										int MIN_NEIGHBORS )  
	{
		super(dao);
		
		this.NEIGHBORHOOD_SIZE = NEIGHBORHOOD_SIZE;
		this.MIN_NEIGHBORS = MIN_NEIGHBORS;
		
		neighbors = new HashMap<Integer, ArrayList<Integer>>();
		similarity = new HashMap<Integer, HashMap<Integer, Double>>();
	}
	
	/**
	 * This method overrides the train() method in the BaseRecommender class and finds neighbors for each user.
	 */
	public void train ()	
	{
		dao.userMeanNormalize();
		computeSimilaritybwAllPairs();		
		
		// Find neighborhood for each user
		for (Integer user : dao.getTrainUsers())			
			neighbors.put(user, findNeighbors(user));
	}

	/**
	 * This method overrides the predict() method in the BaseRecommender class.
	 * @return the predicted rating that @user will assign to @item, or Double.NEGATIVE_INFINITY 
	 * if a prediction cannot be made
	 */
	public double predict (	int user, 
							int item )	
	{
		double prediction = 0.0, normalizer = 0.0;
		int countRatings = 0;
		
		for (Integer neighbor : neighbors.get(user))	{
			if (dao.containsTrainRating(neighbor, item))	{
				double rating = dao.getTrainRating(neighbor, item);
				double sim = getUUSimilarity(user, neighbor);
				prediction += rating * sim;
				normalizer += Math.abs(sim);
				countRatings++;
			}
		}
		
		if (countRatings >= MIN_NEIGHBORS)
			return ceilPrediction(dao.getUserMeanRating(user) + ((prediction + 1) / (normalizer + 1)));
		return Double.NEGATIVE_INFINITY;
	}
	
	
	/*********************************************************************************************************/
	/* 										Private helper methods											 */
	/*********************************************************************************************************/
	
	/**
	 * @return the vector cosine similarity between rating vectors of users @u and @v, @u < @v
	 */
	private double VectorCosineSimilarity (	int u, 
											int v )	
	{
		double num = 0.0, norm_u = 0.0, norm_v = 0.0;
		
		for (Integer item : dao.getTrainItems(u))	{
			if (dao.containsTrainRating(v, item))	{
				double r_u = dao.getTrainRating(u, item);
				double r_v = dao.getTrainRating(v, item);
				num += r_u * r_v;
				norm_u += r_u * r_u;
				norm_v += r_v * r_v;
			}
		}
		norm_u = Math.sqrt(norm_u);
		norm_v = Math.sqrt(norm_v);
		return (num + 1) / (norm_u * norm_v + 1);
	}
	
	/**
	 * @return the vector cosine similarity between rating vectors of users @u and @v
	 * Since vector similarity is symmetric, it needs to be calculated only for one half
	 * of all the user pairs (u, v).
	 */
	private double getUUSimilarity (int u, 
									int v )	
	{
		if (u < v)
			return similarity.get(u).get(v);
		else if (u == v)
			return 1.0;
		else
			return similarity.get(v).get(u);
	}
	
	/**
	 * Computes and stores vector cosine similarity between all user pairs (u, v).
	 * Since vector similarity is symmetric, it needs to be calculated only for one half
	 * of all the user pairs (u, v).
	 */
	private void computeSimilaritybwAllPairs ()	
	{
		for (Integer u : dao.getTrainUsers())	{
			for (Integer v : dao.getTrainUsers())	{
				if (u < v)	{
					double sim = VectorCosineSimilarity(u, v);
					if (similarity.containsKey(u))
						similarity.get(u).put(v, sim);
					else	{
						HashMap<Integer, Double> sim_score = new HashMap<Integer, Double>();
						sim_score.put(v, sim);
						similarity.put(u, sim_score);
					}
				}
			}
		}
	}
	
	/**
	 * @return a list, of size at most @NEIGHBORHOOD_SIZE, of most similar neighbors of @user
	 */
	private ArrayList<Integer> findNeighbors (int user)	
	{
		// A minimum priority queue to permit replacing a neighbor with the least similar
		// neighbor already in the priority queue for some @user
		PriorityQueue<Point> pq = new PriorityQueue<Point>(NEIGHBORHOOD_SIZE, new Comparator<Point>() {
			public int compare(Point a, Point b)	{
				double sim_u1 = getUUSimilarity(a.x, a.y);
				double sim_u2 = getUUSimilarity(b.x, b.y);
				if (sim_u1 < sim_u2)
					return -1;
				else if (sim_u1 == sim_u2)
					return 0;
				else
					return 1;
			}
		});
		
		for (Integer v : dao.getTrainUsers())	{
			if (v != user)	{
				if (pq.size() < NEIGHBORHOOD_SIZE) {
						pq.add(new Point(user, v));
				} else {
					Point min = pq.peek();
					if (getUUSimilarity(min.x, min.y) < getUUSimilarity(user, v))	{
						pq.remove();
						pq.add(new Point(user, v));
					}
				}
			}
		}
					
		ArrayList<Integer> nbrs = new ArrayList<Integer>();
		for (Point a : pq)
			nbrs.add(a.y);
		return nbrs;
	}	
}
