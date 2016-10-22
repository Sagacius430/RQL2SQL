package br.edu.ifsp.syntacticTree;

import br.edu.ifsp.parser.Token;

/**
 * Class that represents a general node
 * @author Lucas Venezian
 */
public abstract class Node {

   private Token position;
   public int number;
   
   public Node( Token position ) {
	   this.position = position;
   }
   
   public Token getPosition() {
	   return this.position;
   }
   
   public void setPosition( Token position ) {
	   this.position = position;
   }
   
   public abstract Token getFirstToken();
}