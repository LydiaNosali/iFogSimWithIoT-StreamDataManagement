BasicDelayMatrix:
  - creates a matrix containing the time (in milliseconds) required to transfer the data Di from producer Pi to its destination storage node Hj. 
  - creates a matrix containing the time (in milliseconds) required to transfer the data Di from storage node Hj to its destination consumer Ck. 

ConsProdMatrix:
  - creates a consumer subscription matrix that relates each Di to its consumers Ck.

MakeLPfile makes the CPLEX LP file of the GAP problem. It uses the class FindFactorsThread that calculates the factors of variables of the GAP problem.
