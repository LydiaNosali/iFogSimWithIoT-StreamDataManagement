package org.fog.lpFileConstuction;



import java.io.IOException;

import org.fog.examples.Log;

import org.fog.stats.*;

public class FindFactorsThread extends Thread{
	
	private  double [][] fact;
	private int begin;
	private int end;
	public FindFactorsThread(String name, int begin, int end){
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
		
		//première demande max(p(x*(ts_j+tr_k)))
		for(int i=begin;i<end;i++){
			//Calcul du nombre de paquets à envoyer Si/b
			x=(int) (((double)MakeLPFile.dataSize[i])/((double)MakeLPFile.base));
			if((MakeLPFile.dataSize[i])%(MakeLPFile.base)!=0){
				x++;
			}
			//Calcul de ts+ a*tr = write + consprod*read
			for (int j = 0; j < MakeLPFile.nb_DataHost;j++){
				double readDelay = 0 ;
				
				//Calcul de ts
				double writeDelay = (MakeLPFile.write_basis_laenty[j][i]);
				
				//Calcul de ts+ a*tr = write + consprod*read
				for (int k = 0; k < MakeLPFile.nb_DataCons;k++){
					
					//Calcul de a*tr
					readDelay = MakeLPFile.read_basis_laenty[j][k] * MakeLPFile.consProd_matrix[i][k];
					
					//Calcul de p(x*(ts+a*tr))
					maxp = PenalityStats.penalityIoT(x*(readDelay + writeDelay));
					
					//Calcul sur les hebergeur de max (p(x*(ts+a*tr)))
					if (maxp > fac[indLine][j]) {
							fac[indLine][j]=maxp;
							kc[i] = k;
					}
				}
			}
			indLine++;
		}
		
		indLine = 0;
		//Le reste des demandes du consommateur choisi pour le stockage
		for(int i=begin;i<end;i++){
			//Calcul du nombre de paquets à envoyer Si/b
			x=(int) (((double)MakeLPFile.dataSize[i])/((double)MakeLPFile.base));
			if((MakeLPFile.dataSize[i])%(MakeLPFile.base)!=0){
				x++;
			}
			//Calcul de nk*a*tr = popularity*consprod*read
			for (int j = 0; j < MakeLPFile.nb_DataHost;j++){
				double readDelay = 0 ;
				
				//Calcul de nk*a*tr = popularity*consprod*read
				readDelay = (MakeLPFile.popularity[i][kc[i]]-1)*MakeLPFile.read_basis_laenty[j][kc[i]]*MakeLPFile.consProd_matrix[i][kc[i]];
				//Calcul de p(x*nk*a*tr) = p(x*popularity*consprod*read)
				fac[indLine][j] = fac[indLine][j] + PenalityStats.penalityIoT(x*readDelay);
			}
			indLine++;
		}			
		
		
		//le reste des demandes du reste des consommateurs	
		indLine = 0;
		for(int i=begin;i<end;i++){
			//Calcul du nombre de paquets à envoyer Si/b
			x=(int) (((double)MakeLPFile.dataSize[i])/((double)MakeLPFile.base));
			if((MakeLPFile.dataSize[i])%(MakeLPFile.base)!=0){
				x++;
			}
			//Calcul de nk*a*tr = popularity*consprod*read
			for (int j = 0; j < MakeLPFile.nb_DataHost;j++){
				double readDelay = 0 ;
				
				//Calcul de nk*a*tr = popularity*consprod*read
				for (int k = 0; k < MakeLPFile.nb_DataCons;k++){
					if (k!=kc[i]) 
						fac[indLine][j] = fac[indLine][j]+PenalityStats.penalityIoT(x*MakeLPFile.popularity[i][k]*MakeLPFile.read_basis_laenty[j][k]*MakeLPFile.consProd_matrix[i][k]);
				}
			}
		indLine++;
		}
		fact = fac;	
	}
	

}


















