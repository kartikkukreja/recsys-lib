recsys-lib
==========

A simple Java library for using, building and experimenting with different recommender systems
Simply import the project in Eclipse to run it.

I wrote this library for a school project. It contains implementations of the following recommender algorithms:
* User - User collaborative filtering with vector cosine similarity
* Item - Item collaborative filtering with vector cosine similarity
* Slope one item-based recommender algorithm
* Funk Singular Value Decomposition recommender algorithm

New recommender algorithms can be developed by extending the BaseRecommender class.

I tested the algorithms on Movielens 100K dataset, which can be downloaded from here: http://grouplens.org/datasets/movielens/

Please refer to doc/index.html for documentation.
