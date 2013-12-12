package recommender;

import java.io.IOException;

/**
 * Sample Class that instantiates different recommenders for movielens 100K data set
 * and outputs their performance statistics for comparison
 */
public class CompareRecommenders {

	public static void main ( String[] args ) 
			throws IOException	
	{
		BaseRecommender baserec = null;	
		
		System.out.println("Creating data access object...");
		DAO dao = new DAO("data/u1.base", "data/u1.test", "\t");
		System.out.println("Data access object created.");
		
		System.out.println("\nBase Recommender");
		baserec = new BaseRecommender(dao);
		System.out.println("Training...");
		baserec.train();
		System.out.println("Testing...");
		baserec.evaluate("data\\baserec.predict");
		
		System.out.println("\nUser User Collaborative Filtering Recommender");
		baserec = new UUCollaborativeFiltering(dao, 378, 12);
		System.out.println("Training...");
		baserec.train();
		System.out.println("Testing...");
		baserec.evaluate("data\\uucf.predict");
		dao.userMeanDeNormalize();
		
		System.out.println("\nItem Item Collaborative Filtering Recommender");
		baserec = new IICollaborativeFiltering(dao, 1116, 10);
		System.out.println("Training...");
		baserec.train();
		System.out.println("Testing...");
		baserec.evaluate("data\\iicf.predict");
		dao.itemMeanDeNormalize();
		
		System.out.println("\nSlope One Recommender");
		baserec = new SlopeOneRecommender(dao);
		System.out.println("Training...");
		baserec.train();
		System.out.println("Testing...");
		baserec.evaluate("data\\slopeone.predict");
		
	
		System.out.println("\nFunk SVD Recommender");
		baserec = new FunkSVDRecommender(dao, 20, 100, 0.1, 0.0001, 0.001, 0.015, "data\\funksvd_users_1.features", "data\\funksvd_movies_1.features");
		//baserec = new FunkSVDRecommender(dao, "data\\funksvd_users_4.features", "data\\funksvd_movies_4.features");
		System.out.println("Training...");
		baserec.train();
		System.out.println("Testing...");
		baserec.evaluate("data\\funksvd.predict");
	
	}
}
