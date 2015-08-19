cd 2D_DataGenerator

#Number of Points
b=10

#Number of Cluster
k=10


		echo ********GENERATING $b INPUT POINTS EACH IN $k CLUSTERS 
		python generaterawdata.py -c $k  -p $b -o cluster.csv

cd ..

