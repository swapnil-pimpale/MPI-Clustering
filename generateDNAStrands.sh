cd DNA_DataGenerator

javac *.java
jar cvfe DNAGenerator.jar DNAStrandGenerator *.class


#Number of Strands per Cluster
b=10

#Number of Clusters
k=10

#Length of DNA Strand
len=26


		echo ********GENERATING $b STRANDS PER CLUSTER, WITH A TOTAL OF $k CLUSTERS 
		java -jar DNAGenerator.jar $b $k $len

cd ..

