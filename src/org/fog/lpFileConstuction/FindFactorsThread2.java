package org.fog.lpFileConstuction;



import java.io.IOException;

import org.fog.examples.Log;

import org.fog.stats.*;

public class FindFactorsThread2 extends Thread{
	
	private  double [][] fact;
	private int begin;
	private int end;
	public FindFactorsThread2(String name, int begin, int end){
		super(name);
		this.begin=begin;
		this.end=end;
	}
	
	
	public double[][] getFact(){
		return this.fact;
	}
	
	public void run(){
		int x;
		double [][] fac = new double [end-begin][MakeLPFile.nb_DataHost];
		int [] kc = new int [MakeLPFile.nb_DataCons];
		double maxp=0;
		int indLine = 0;
		

		for(int i=begin;i<end;i++){
			
			//Calcul du nombre de paquets à envoyer Si/b
			x=(int) (((double)MakeLPFile.dataSize[i])/((double)MakeLPFile.base));
			
			if((MakeLPFile.dataSize[i])%(MakeLPFile.base)!=0){
				x++;
			}
			
			//Calcul de x (ts+ \Sum a*tr)
			for (int j = 0; j < MakeLPFile.nb_DataHost;j++){
				double readDelay = 0 ;
				
				//Calcul de ts
				double writeDelay = x* (MakeLPFile.write_basis_laenty[j][i]);
				
				
				//popularity*consprod*read * x
				for (int k = 0; k < MakeLPFile.nb_DataCons;k++){
					readDelay = readDelay + x * (MakeLPFile.popularity[i][k] * MakeLPFile.read_basis_laenty[j][k] * MakeLPFile.consProd_matrix[i][k]);
				}
								
				fac[indLine][j]=readDelay + writeDelay;
			}
			indLine++;
		}
		fact = fac;	
	}
	

}


















