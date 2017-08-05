JLibFM
=====

A Java implementation of libFM: Factorization Machine Library

### Description ###
Factorization machines (FM) are a generic approach that allows to mimic most factorization models by feature engineering. This way, factorization machines combine the generality of feature engineering with the superiority of factorization models in estimating interactions between categorical variables of large domain. libFM is a software implementation for factorization machines that features stochastic gradient descent (SGD) and alternating least squares (ALS) optimization as well as Bayesian inference using Markov Chain Monte Carlo (MCMC). 
JLibFM is the Java version of [LibFM](http://www.libfm.org/).

### Dependencies and requirements ###
Please note that the code is written in [Java](https://www.oracle.com/java/index.html), and this project is a [Maven](https://maven.apache.org/) project.

* In addition, this project has no third party dependency

### How to run ###
Please go to the project folder and run the command 
"mvn clean package -Dmaven.test.skip=true", then we will get two archive files in the sub folder "target", one is 
"JLibFM-0.1-SNAPSHOT-jar-with-dependencies.jar".
Now we can prepare the dataset. In the current version, only [LibSVM format](http://www.csie.ntu.edu.tw/~cjlin/libsvm/faq.html) is supported.
There is a Java class com.github.gaterslebenchen.libfm.examples.MovieLens1MFormater in this project, which shows us how to format [MovieLens 1M Dataset](https://grouplens.org/datasets/movielens/1m/) to [LibSVM format](http://www.csie.ntu.edu.tw/~cjlin/libsvm/faq.html).

(1) an example of Stochastic Gradient Descent (SGD) method:

    * java -Xms1024M -Xmx2048M -jar JLibFM-0.1-SNAPSHOT-jar-with-dependencies.jar -task r -train ratings_train.libfm -test ratings_test.libfm -dim 1,1,8 -iter 30 -method sgd -learn_rate 0.01 -regular 0,0,0.1 -init_stdev 0.1 -rlog log.txt -verbosity 1

(2) an example of Alternating Least Squares (ALS) method:

    * java -Xms1024M -Xmx2048M -jar JLibFM-0.1-SNAPSHOT-jar-with-dependencies.jar -task r -train ratings_train.libfm -test ratings_test.libfm -dim 1,1,8 -iter 20 -method als -regular 0,0,10 -init_stdev 0.1 -rlog log.txt -verbosity 1

(3) an example of Markov Chain Monte Carlo (MCMC) method:

    * java -Xms1024M -Xmx2048M -jar JLibFM-0.1-SNAPSHOT-jar-with-dependencies.jar -task r -train ratings_train.libfm -test ratings_test.libfm -dim 1,1,8 -iter 20 -method mcmc -init_stdev 0.1 -rlog log.txt -verbosity 1
    
(4) an example of Adaptive SGD (SGDA) method:

    * java -Xms1024M -Xmx2048M -jar JLibFM-0.1-SNAPSHOT-jar-with-dependencies.jar -task r -train ratings_train.libfm -test ratings_test.libfm -validation ratings_valid.libfm -dim 1,1,8 -iter 20 -method sgda  -init_stdev 0.1 -learn_rate 0.01 -rlog log.txt -verbosity 1
    
### Evaluation data ### 
I built the dataset with [MovieLens 1M Dataset](https://grouplens.org/datasets/movielens/1m/).
Here are some evaluation data:

<table>
  <thead>
    <tr>
      <th style="width: 100px;">Method</th>
      <th style="width: 50px;">RMSE</th>
      <th style="width: 50px;">Dim</th>
      <th style="width: 50px;">Iter</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td>MCMC</td>
      <td>0.8606</td>
      <td>1,1,8</td>
      <td>20</td>
    </tr>
    <tr>
      <td>ALS</td>
      <td>0.8511</td>
      <td>1,1,8</td>
      <td>20</td>
    </tr> 
    <tr>
      <td>SGD</td>
      <td>0.8838</td>
      <td>1,1,8</td>
      <td>30</td>
    </tr> 
     <tr>
      <td>SGDA</td>
      <td>0.9046</td>
      <td>1,1,8</td>
      <td>20</td>
    </tr> 
  </tbody>
</table>

### How to save and load model ###
The JUnit TestCase com.github.gaterslebenchen.libfm.TestSaveandLoadModel shows how to save and load model for SGD method.