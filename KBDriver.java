//Author: Jackson Mishuk

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Scanner;

public class KBDriver {
	public static void main(String[] args) {
		Problem P;
		if(args.length != 0) {
			P = new Problem(args[0]);
			if(!P.loadFile()) {
				System.out.println("The File is not valid!"); 
				return;
			}
			try (BufferedReader br = new BufferedReader(new FileReader(P.filePath))){
				String line = br.readLine();
				while(line != null) {
					if(line.length() == 0 || line.charAt(0)=='#') {
						line = br.readLine();
						continue;
					}
					System.out.println("> " + line);
					checkCmd(line, P);
					line = br.readLine();
				}
					
			}catch(Exception e) {
				e.printStackTrace();
			}
		}else {
			P = new Problem();
			Scanner s = new Scanner(System.in);
			System.out.println("Welcome to the Knowledge Base!\n"
					+ "Please TELL or ASK me anything!\n"
					+ "(type HELP for more information)");
			
			while(true) {
				System.out.print("> ");
				String input = s.nextLine();
				if(!checkCmd(input, P)) {
					break;
				}
			}
			return;
		}
	}
	

	/*
	 * This method is used to check an inputed argument
	 * 
	 * Parameter(s): String(input), Problem(P)
	 * 
	 * Returns: Boolean of success or failure
	 */
	private static boolean checkCmd(String input, Problem P) {

		String firstArg="";
		String secondArg="";
		
		int firstSpaceIndex = input.indexOf(" ");
		if(firstSpaceIndex != -1) {
			firstArg = input.substring(0, firstSpaceIndex).toUpperCase();
			secondArg = input.substring(firstSpaceIndex+1);
		}else 
			firstArg = input.toUpperCase();
		
		if(firstArg.equals("HELP")) 
			System.out.println(helpString());
		
		else if(firstArg.equals("PRINT"))
			System.out.print(argsString(P));
		
		else if(firstArg.equals("TELLC")) {
			Problem.Clause newClause = tellc(secondArg, P);
			if(newClause!=null) {
				for(Problem.Clause element:P.clauseList) {
					if(element.hasSameLiterals(newClause))
						return true;
				}
				P.clauseList.add(newClause);
			}
			
		}else if(firstArg.equals("ASK")) {
			if(resolution(P.clauseList, secondArg, P)!=null) System.out.println("Yes, KB entails " + secondArg);
			else System.out.println("No, KB does not entail " + secondArg);
		}
		else if(firstArg.equals("PROOF")) {
			Problem.Clause finalClause = resolution(P.clauseList, secondArg, P);
			if(finalClause!=null) {
				System.out.println("Proof:\n" + getProof(finalClause));
			}
			else System.out.println("No proof exists");
		
		}else if(firstArg.equals("PARSE"))
			new ParseTree(secondArg, true/*parse print*/);
		else if(firstArg.equals("CNF")) {
			ParseTree Tree = new ParseTree(secondArg, false/*parse print*/);
			Tree.convertToCnfForm();
			System.out.println(Tree.toStr());
		}
		
		else if(firstArg.equals("TELL")) {
			
			ParseTree Tree = new ParseTree(secondArg, false/*parse print*/);
			Tree.convertToCnfForm();
			String[] clauseArr = Tree.getClauses();
			for(String element:clauseArr) {
				Problem.Clause newClause = tellc(element, P);
				P.clauseList.add(newClause);
			}
			
		}
		
		else if(firstArg.equals("DONE") || firstArg.equals("EXIT") || firstArg.equals("QUIT")) {
			System.out.println("Thank you for using the Knowledge Base!");
			return false;
		}
		
		else
			System.out.println("That command is not supported!");
		
		return true;
	}
	
	/*
	 * Called when the HELP command is used
	 * 
	 * Parameter(s): none
	 * 
	 * Returns: String(List of all supported commands)
	 */
	private static String helpString() {
		String ret = "\nSUPPORTED COMMANDS:\n\n";
		ret 			+= "\tHELP            : Prints this help message\n";
		ret 			+= "\tDONE/EXIT/QUIT  : Terminates the session\n";
		ret 			+= "\tTELLC <clause>  : Adds the given <clause> to \n";
		ret 			+= "\tPRINT           : Prints the clauses currently in \n";
		ret 			+= "\tASK <query>     : Determines if the KB entails <query>.\n";
		ret 			+= "\tPROOF <query>   : Prints a proof of <query> from \n";
		ret 			+= "\tPARSE <sentence>: Prints the parse tree of the given <sentence>.\n";
		ret 			+= "\tCNF <sentence>  : Prints <sentence> in conjunctive normal form.\n";
		ret 			+= "\tTELL <sentence> : Adds the clauses in the CNF representation of <sentence> to \n";
		
		return ret;
	}
	
	/*
	 * Called when the PRINT command is used
	 * 
	 * Parameter(s): Problem
	 * 
	 * Returns: String(All given clauses)
	 */
	private static String argsString(Problem P) {
		String ret = "";
		
		for(int i = 0; i<P.clauseList.size(); i++)
			ret += "("+P.clauseList.get(i).toStr()+")" + "\n";
		return ret;
	}
	
	/*
	 * Called when the TELLC command is used
	 * 
	 * Parameter(s): String(clause), Problem
	 * 
	 * Returns: boolean
	 */
	private static Problem.Clause tellc(String clauseStr, Problem P){
		
		Problem.Clause newClause = new Problem.Clause(clauseStr, P);

		return newClause;
	}

	static HashMap<Problem.Clause, Integer> map = new HashMap<Problem.Clause, Integer>();
	
	/*
	 * Receives an empty Clause after doing an ask and gets the steps to the array, orders them, and returns the String
	 * 
	 * Parameter(s): Clause(clause); 
	 * 
	 * Returns: String(of the proof)
	 */
	private static String getProof(Problem.Clause clause) {
		
		String retStr = "";
		
		ArrayList<Problem.Clause> proofArr = new ArrayList<Problem.Clause>(); 
		proofArr = getParentClauses(clause, proofArr);
		
		Collections.sort(proofArr, new Comparator<Problem.Clause>() {
			public int compare(Problem.Clause clause1, Problem.Clause clause2) {
				if(clause1.createdBy1 == null && !clause1.negatedGoal) {
					if(clause2.createdBy1 == null && !clause2.negatedGoal)return 0;
					else return -1;
				}
				if(clause2.createdBy1 == null && !clause2.negatedGoal) return 1;
				
				
				if(clause1.createdBy1 != null) {
					if(clause2.createdBy1 != null)return 0;
					else return 1;
				}
				
				if(clause2.createdBy1 != null)return -1;
				
				return 0;
			}
		});
		
		Problem.Clause element = null;
		for(int i = 0; i<proofArr.size(); i++) {
			element = proofArr.get(i);
			retStr +=  String.format(" %-3s %s", String.valueOf(i+1)+".",  element.getProofStr(map));
			map.put(element, i+1);
		}
		
		return retStr;
	}
	
	/*
	 * Receives an empty Clause after doing an ask and gets the Clauses that lead to the creation of the empty Clause
	 * 
	 * Parameter(s): Clause(clause), ArrayList<Clause>(proofArr)
	 * 
	 * Returns: ArrayList<Clause>(parents of inputed Clause)
	 */
	private static ArrayList<Problem.Clause> getParentClauses(Problem.Clause clause, ArrayList<Problem.Clause> proofArr){

		if(clause == null) return proofArr;
		
		
		getParentClauses(clause.createdBy1, proofArr);
		getParentClauses(clause.createdBy2, proofArr);
		
		for(Problem.Clause cElement: proofArr) {
			if(clause.hasSameLiterals(cElement)) {
				proofArr.remove(cElement);
				break;
			}
		}
		proofArr.add(clause);
		
		return proofArr;
	}
	
	/*
	 * Receives a list of Clauses and a goal Clause and attempts to prove that the list entails the goal
	 * 
	 * Parameter(s): ArrayList<Clause>(knowledgeBase), String(alpha), Problem(P)
	 * 
	 * Returns: Clause(The empty Clause at the end if it is entailed or null if it is not)
	 */
	private static Problem.Clause resolution(ArrayList<Problem.Clause> knowledgeBase, String alpha, Problem P) {
		
		ArrayList<Problem.Clause> clauses = new ArrayList<Problem.Clause>(P.clauseList);
		
		ParseTree Tree = new ParseTree("~("+alpha+")", false/*parse print*/);
		Tree.convertToCnfForm();
		String[] strNegatedGoal = Tree.getClauses();
		
		for(String strElement:strNegatedGoal) {
			Problem.Clause tempClause = tellc(strElement, P);
			tempClause.negatedGoal = true;
			clauses.add(0, tempClause);
		}
		
		ArrayList<Problem.Clause> newClauses = new ArrayList<Problem.Clause>();
		
		while(true) {
			for(int i = 0; i<clauses.size(); i++) {
				for(int j = i+1; j<clauses.size(); j++) {
					ArrayList<Problem.Clause> resolvents = resolve(clauses.get(i), clauses.get(j), P);
					for(Problem.Clause element:resolvents) {
						if(element.emptyClause)
							return element; 
						newClauses.addAll(resolvents);
					}
				}
			}			
			int clausesSize = clauses.size();
			int i = 0;
			for(; i<newClauses.size(); i++) {
				Problem.Clause newElement = newClauses.get(i);
				int j = 0;
				for(; j<clausesSize; j++) {
					if(newElement.hasSameLiterals(clauses.get(j))) {
						j = clausesSize;
						continue;
					}
					if(j+1==clauses.size())
						clauses.add(newElement);
				}
			}
			if(clausesSize == clauses.size())return null;
		}
	}
	
	/*
	 * Returns a list of Clauses that can be created with the two input Clauses
	 * 
	 * Parameter(s): Clause(clauseI), Clause(clauseJ), Problem(P)
	 * 
	 * Returns: ArrayList<Clause>(list of clauses entailed by clauseI & clauseJ)
	 */
	private static ArrayList<Problem.Clause> resolve(Problem.Clause clauseI, Problem.Clause clauseJ, Problem P){
		
		ArrayList<Problem.Clause> retList = new ArrayList<Problem.Clause>();
		
		Problem.Clause.Literal[] clauseIArr = clauseI.Literals.toArray(new Problem.Clause.Literal[0]);
		Problem.Clause.Literal[] clauseJArr = clauseJ.Literals.toArray(new Problem.Clause.Literal[0]);
		
		for(int i = 0; i < clauseIArr.length; i++) {
			for(int j = 0; j < clauseJArr.length; j++) {
				if(clauseIArr[i].Symbol.equals(clauseJArr[j].Symbol) && clauseIArr[i].negated != clauseJArr[j].negated) {
					Problem.Clause Clause = new Problem.Clause(clauseI, clauseIArr, i, clauseJ, clauseJArr, j, P);
					if(Clause.Literals.isEmpty()) 
						Clause.emptyClause = true;
					retList.add(Clause);
				}
				
			}
		}
		
		return retList;
	}
	
	
	/*
	 * The Problem class is used to hold information about the current run of the code
	 * 
	 * Attributes: String filePath, ArrayList<Literal> literalList, ArrayList<Clause> clauseList, LinkedList<String> fileArgList
	 * 
	 * Constructors: Problem(), Problem(String f)
	 * 
	 * Methods: loadFile()
	 * 
	 * *Contains Class Clause*
	 */
	private static class Problem{
		//String representation of the path to the file
		private String filePath = null;
		
		//Array List of all clauses that have been created
		private ArrayList<Clause> clauseList = new ArrayList<Clause>();
		
		//Only created when the cmd argument is given a file
		//Holds a LinkedList of all of the string(arguments) that were made within the file
		private LinkedList<String> fileArgList = new LinkedList<String>();
		

		//Used to create a Problem for interactive mode
		Problem(){}
		//Used to create a Problem for file mode
		Problem(String f){ 
			filePath = f; 
		}
		
		//Used to read information from the file and adds each individual arg to the fileArgList object
		private boolean loadFile() {
			try (BufferedReader br = new BufferedReader(new FileReader(filePath))){
				String line = br.readLine();
				while(line != null) {
					this.fileArgList.add(line);
					line = br.readLine();
				}
			} catch (Exception e) {return false;} 
			return true;
		}
		
			
		/*
		 * The Clause Class is for clauses which are literals(Symbols/negated symbols) with only or(v) connectives
		 * 
		 * Attributes: PriorityQueue<Literal> Literals(Ordered in Lexicographical order), boolean emptyClause, boolean negatedGoal,
		 * 						 Clause createdBy1, Clause createdBy2, LinkedList<Clause> created, String literalRemoved
		 * 
		 * Constructors: Clause(Clause clauseI, Literal[] set1, int i, Clause clauseJ, Literal[] set2, int j, Problem P),
		 * 							 Clause(String s, Problem P)
		 * 
		 * Methods: hasSameLiterals(Clause): Checks to see if a Clause has the same set of literals that this Clause has, 
		 * 					toStr(): Returns the String representation of the clause,
		 * 					getProofStr(HashMap<Clause, Integer>): Returns the line of a proof represented by the clause
		 * 
		 * *Contains Class Literal*
		 */
		static private class Clause{

			//Holds a list of the Literals of the clause(these statements must be ored together due to the definition of a clause in PL)
			private PriorityQueue<Literal> Literals = new PriorityQueue<Literal>(new Comparator<Literal>() {
				public int compare(Literal i1,Literal i2){  
					String s1 = i1.Symbol;
					String s2 = i2.Symbol;
					
					return s1.compareTo(s2);
				}
			});
			
			//These variables are used for refutation method clauses only
			//If it is the final state proving a contradiction in a refutation method
			private boolean emptyClause = false;
			//If created from the inverse of a goal state within the refutation method
			private boolean negatedGoal = false;
			//The clauses that entail this created clause
			private Clause createdBy1 = null;
			private Clause createdBy2 = null;
			//When the clause is created through resolution of two clauses, this is the Symbol that is "Cancelled"
			private String literalRemoved = "";
			
			/*
			 * This constructor should only be called when dealing with Resolution
			 * 
			 * It combines the literals from both sets excluding the index i for set 1 and index j for set 2
			 */
			Clause(Clause clauseI, Literal[] set1, int i, Clause clauseJ, Literal[] set2, int j, Problem P){
				for(int k = 0; k <set1.length; k++) {
					if(k != i)
						Literals.add(set1[k]);
				}
				for(int k = 0; k <set2.length; k++) {
					if(k != j && !Literals.contains(set2[k])) //to deal with repeats
						Literals.add(set2[k]);
				}

				for(Clause cElement:P.clauseList) {
					if(hasSameLiterals(cElement)) cElement = this;
				}
				
				createdBy1 = clauseI;
				createdBy2 = clauseJ;
				literalRemoved = set1[i].Symbol;
				
				
			}
			
			/*
			 * This is the standard constructor for clauses using a string representation of a clause
			 */
			Clause(String s, Problem P){
				String[] literalArr = s.split("v");
				StringBuilder substrElement;
				
				for(String element:literalArr) {
					boolean alreadyCreated = false;
					
					boolean negated = false;
					int negationIndex = element.indexOf("~");
					
					if(negationIndex != -1) {
						negated = true;
						substrElement = new StringBuilder(element.substring(negationIndex+1));
					}else
						substrElement = new StringBuilder(element);
					
					for(int i = 0; i<substrElement.length(); i++) {
						if(substrElement.charAt(i) == ' ') { substrElement.deleteCharAt(i); i--;}
					}
					
					Literals.add(new Literal(substrElement.toString(), negated));
				}
				
			}
			
			/*
			 * Checks to see if a Clause has the same set of literals that this Clause has
			 * 
			 * Parameter(s): Clause(element)
			 * 
			 * Returns: Boolean(True if same, false if not)
			 */
			private boolean hasSameLiterals(Clause element) {
				
				if(element.Literals.size() != this.Literals.size())return false;
				
				PriorityQueue<Literal> thisLiteralClone = new PriorityQueue<Literal>(this.Literals);
				PriorityQueue<Literal> literalsClone = new PriorityQueue<Literal>(element.Literals);

				while(literalsClone.peek()!=null) {
					Literal thisLiteral = thisLiteralClone.poll();
					Literal literal = literalsClone.poll();
					if(!literal.Symbol.equals(thisLiteral.Symbol) || literal.negated != thisLiteral.negated) {
						return false;
					}
				}
				return true;
			}
			
			
			/*
			 * Returns the String representation of the clause
			 * 
			 * Parameter(s): None
			 * 
			 * Returns: String
			 */
			private String toStr(){
				String retStr = "";
				
				PriorityQueue<Literal> literalsCopy = new PriorityQueue<Literal>(Literals);//This array is not in particular order. Need to find a new way.//This array is not in particular order. Need to find a new way.
				
				if(literalsCopy.peek()!=null) {
					
					Literal element = literalsCopy.poll();
					
					retStr += element.toStr();
					
					while(literalsCopy.peek() != null) {
						element = literalsCopy.poll();
						retStr += " v ";
						retStr += element.toStr();
					}
				}else return "()";
				
				return retStr;
			}
			
			/*
			 * Returns the line of a proof represented by the clause
			 * 
			 * Parameter(s): HashMap<Problem.Clause, Integer>(map)
			 * 
			 * Returns: Boolean(True if same, false if not)
			 */
			private String getProofStr(HashMap<Problem.Clause, Integer> map) {
				if(createdBy1 == null) {
					if(negatedGoal)
						return String.format("%-30s[Negated Goal]\n", toStr());
					return String.format("%-30s[Premise]\n", toStr());
				}
				return String.format("%-30s[Resolution on %s: %d, %d]\n", toStr(), literalRemoved, map.get(createdBy1), map.get(createdBy2));
			}
			
			/*
			 * The Literal Class is used to store the Symbol and to allow the database to store if that Symbol is negated
			 * 
			 * Attributes: String Symbol; boolean negated
			 * 
			 * Constructor: Literal(String s, boolean n, Problem P)
			 * 
			 * Method: toStr(): Returns the String representation of the Literal
			 */
			static private class Literal{
				String Symbol;
				boolean negated;
				
				//Used to create a Literal with Symbol s with negation n
				Literal(String s, boolean n) {
					Symbol = s;
					negated = n;
				}
				
				//Returns the String representation of the Literal
				private String toStr() {
					String retStr = "";
					if(this.negated)
						retStr += "~";
					retStr += this.Symbol;
					
					return retStr;
				}
			}
		}
	}
	
	/*
	 * The ParseTree Class is used to apply an order of operations to Propositional Logic 
	 * 
	 * Attributes: Node root, static boolean parsePrint, boolean unarySentance
	 * 
	 * Constructor: makeParseTree(String arg, int distRoot)
	 * 
	 * Method: getClauses(): , toTreeString(Node), convertToCnfForm(), convertToCnfForm(Node), toStr(), toStr(Node, String) 
	 * 
	 * *Contains Class Node*
	 */
	private static class ParseTree{
		//The root is the Node in the tree that has no parents. In other words, all Nodes in the ParseTree are direct decendents of this Node
		private Node root = null;
		
		//This is true only when a ParseTree is created from the Parse command and prints out the parsing steps as it expands a string
		private static boolean parsePrint;
		
		//Used for toString() when the sentence consists of only 1 clause
		private boolean unarySentance = true;
		
		//This constructor is used to make a Parse Tree from String arg
		ParseTree(String arg, boolean pp){
			parsePrint = pp;
			arg = arg.replace(" ", "");
			if(parsePrint) System.out.print("Orig: ");
			root = makeParseTree(arg, 0);
		}
		
		/*
		 * PRE: Should only be called with an argument without any spaces. Call through constructor
		 * 
		 * Called from the constructor to make a parseTree
		 * 
		 * Parameter(s): String(arg), int(distRoot)
		 * 
		 * Returns: Node(Root node of parse tree
		 */
		private static Node makeParseTree(String arg, int distRoot) {
			Node retNode = null;
			
			int openParensCount = 0;
			for(int index = 0; index<arg.length(); index++) {
				char element = arg.charAt(index);
				if(element == '(') {
					openParensCount++;
					continue;
				}
				if(element ==')') {
					openParensCount--;
					continue;
				}
				if(openParensCount == 0) {
					
					//Used to deal withif and only if(<=>) connective
					if(element == '<' && arg.charAt(index+1) == '=' && arg.charAt(index+2) == '>') {
						retNode = new Node("<=>");
						
						//Printing for parse command call
						if(parsePrint) {
							System.out.printf("[%s] Binary [<=>]\n", arg);
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("LHS: ");
						}
							
						retNode.addLeft(makeParseTree(arg.substring(0, index), distRoot+1));
						
						//Printing for parse command call
						if(parsePrint) {
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("RHS: ");
						}
						
						retNode.addRight(makeParseTree(arg.substring(index+3), distRoot+1));
						
						return retNode;
					}
					
					//Used to deal with implies(=>) connective
					if(element == '=' && arg.charAt(index+1) == '>') {
						retNode = new Node("=>");
						
						//Printing for parse command call
						if(parsePrint) {
							System.out.printf("[%s] Binary [=>]\n", arg);
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("LHS: ");
						}
						
						retNode.addLeft(makeParseTree(arg.substring(0, index), distRoot+1));
						
						//Printing for parse command call
						if(parsePrint) {
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("RHS: ");
						}
						
						retNode.addRight(makeParseTree(arg.substring(index+2), distRoot+1));
						return retNode;
					}
					
					//Used to deal with and(^) connective
					if(element == '^') {
						retNode = new Node("^");
						
						//Printing for parse command call
						if(parsePrint) {
							System.out.printf("[%s] Binary [^]\n", arg);
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("LHS: ");
						}
						
						retNode.addLeft(makeParseTree(arg.substring(0, index), distRoot+1));
						
						//Printing for parse command call
						if(parsePrint) {
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("RHS: ");
						}
						
						retNode.addRight(makeParseTree(arg.substring(index+1), distRoot+1));
						return retNode;
					}
					
					//Used to deal with or(v) connective
					if(element == 'v') {
						retNode = new Node("v");
						
						//Printing for parse command call
						if(parsePrint) {
							System.out.printf("[%s] Binary [v]\n", arg);
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("LHS: ");
						}
						
						retNode.addLeft(makeParseTree(arg.substring(0, index), distRoot+1));
						
						//Printing for parse command call
						if(parsePrint) {
							for(int i = 0; i<=distRoot; i++)
								System.out.print(" ");
							System.out.print("RHS: ");
						}
						
						retNode.addRight(makeParseTree(arg.substring(index+1), distRoot+1));
						return retNode;
					}
				}
			}
			
			
			//Used to deal with negation(~)
			if(arg.charAt(0) == '~') {
				retNode = new Node("~");
				
				//Printing for parse command call
				if(parsePrint) {
					System.out.printf("[%s] Unary [~]\n", arg);
					for(int i = 0; i<=distRoot; i++)
						System.out.print(" ");
					System.out.print("Sub: ");
				}
				
				retNode.addRight(makeParseTree(arg.substring(1), distRoot+1));
				return retNode;
			}
			
			
			//Used to deal with symbols
			if(arg.charAt(0)!='(') {
				
				//Printing for parse command call
				if(parsePrint)
					System.out.printf("[%s] Unary [symbol]: [%s]\n", arg, arg);
				return new Node(arg);
			}
			
			//Used to deal with parenthesis
			retNode = new Node("()");
			
			//Printing for parse command call
			if(parsePrint) {
				System.out.printf("[%s] Unary [()]\n", arg);
				for(int i = 0; i<=distRoot; i++)
					System.out.print(" ");
				System.out.print("Sub: ");
			}
			
			retNode.addRight(makeParseTree(arg.substring(1, arg.length()-1), distRoot+1));
			return retNode;
		}
		
		/*
		 * PRE: Make sure that the tree has been reformatted to CNF format using convertToCnfForm() before using this method
		 * 
		 * Used to parse clauses out of the CNF formatted tree
		 * 
		 * Parameter(s): None
		 * 
		 * Returns: String[](The String format of the list of Clauses)
		 */
		private String[] getClauses() {
			String treeString = toTreeString(root);
			String[] clauseArr = treeString.split("\\^");
			return clauseArr;
		}
		
		/*
		 * Used to return all of the string forms of the nodes descending from r
		 * 
		 * Parameter(s): Node(r)
		 * 
		 * Returns: String(The String format of the ParseTree)
		 */
		private String toTreeString(Node r) {
			if(r==null) return "";
			String retStr = "";
			
			retStr = toTreeString(r.left);
			if(!r.value.equals("()"))retStr += r.value;
			retStr += toTreeString(r.right);
			
			return retStr;
		}

		/*
		 * Used to convert a ParseTree 
		 * 
		 * Parameter(s): None
		 * 
		 * Returns: ParseTree(The converted ParseTree)
		 */
		private ParseTree convertToCnfForm() {
			root = convertToCnfForm(root);
			String tempStr = "";
			String newTempStr = toTreeString(root);
			while(!tempStr.equals(newTempStr)) {
				tempStr = toTreeString(root);
				root = convertToCnfForm(root);
				newTempStr = toTreeString(root);
			}
			return this;
		}
		
		/*
		 * Used to convert a ParseTree
		 * 
		 * Parameter(s): Node r
		 * 
		 * Returns: Node(root of the converted ParseTree
		 */
		private Node convertToCnfForm(Node r) {
			
			//If root is null then return without performing any actions
			if (r==null) return null;
			
			//Used to convert if and only if(a<=>b) to (a=>b)^(b=>a)
			if(r.value.equals("<=>")) {
				Node newRoot = new Node("^");
				newRoot.left = new Node("()", newRoot);
				newRoot.right = new Node("()", newRoot);
				
				newRoot.left.right = new Node("=>", newRoot.left);
				newRoot.right.right = new Node("=>",newRoot.right);
				
				newRoot.left.right.addLeft(r.left);
				newRoot.left.right.addRight(r.right);
				newRoot.right.right.addLeft(r.right);
				newRoot.right.right.addRight(r.left);
				
				newRoot.parent = r.parent;
				r = newRoot;
			}
			
			//Used to convert implies(a=>b) to (~a)v(b)
			else if(r.value.equals("=>")) {
				Node newRoot = new Node("v");
				newRoot.left = new Node("~", newRoot);
				
				newRoot.left.addRight(r.left);
				newRoot.addRight(r.right);
				
				newRoot.parent = r.parent;
				r = newRoot;
				
				r = convertToCnfForm(r);
				
				return r;
			}
			
			//Used to push in negation
			else if(r.value.equals("~")) {
				
				//Used to find the next node on the right that is not a parentheses
				Node elementRight = r.right;
				while(elementRight.value.equals("()"))
					elementRight = elementRight.right;
				
				//Used to deal with double negation(~(~a)) to (a)
				if(elementRight.value.equals("~")) {
					elementRight.right.parent = r.parent;
					r = elementRight.right;
					
				//Used to deal with demorgan's(~(avb)) to (~a^~b)
				}else if(elementRight.value.equals("v")) {
					Node newRoot = new Node("^");
					newRoot.left = new Node("~", newRoot);
					newRoot.right = new Node("~", newRoot);
					
					newRoot.left.addRight(elementRight.left);
					newRoot.right.addRight(elementRight.right);
					
					newRoot.parent = r.parent;
					r = newRoot;
					
					r = convertToCnfForm(r);
					
					return r;

				//Used to deal with demorgan's(~(a^b)) to (~av~b)
				}else if(elementRight.value.equals("^")) {
					Node newRoot = new Node("v");
					newRoot.left = new Node("~", newRoot);
					newRoot.right = new Node("~", newRoot);
					
					newRoot.left.addRight(elementRight.left);
					newRoot.right.addRight(elementRight.right);
					
					newRoot.parent = r.parent;
					r = newRoot;
					
					r = convertToCnfForm(r);
					
					return r;
				}
			}
			
			//Used to deal with distributive(av(b^c)) or ((b^c)va) to ((avb)^(avc))
			else if(r.value.equals("v")) {
				
				//Used to find the next node on the right that is not a parentheses
				Node elementRight = r.right;
				while(elementRight.value.equals("()"))
					elementRight = elementRight.right;
				
				//Used to find the next node on the left that is not a parentheses
				Node elementLeft = r.left;
				while(elementLeft.value.equals("()"))
					elementLeft = elementLeft.right;
				
				//((b^c)va) to ((avb)^(avc))
				if(elementLeft.value.equals("^")) {
					Node newRoot = new Node("^");
					//left of root
					newRoot.left = new Node("v", newRoot);
					newRoot.left.addLeft(r.right);
					newRoot.left.addRight(elementLeft.left);
					//right of root
					newRoot.right = new Node("v", newRoot);
					newRoot.right.addLeft(r.right);
					newRoot.right.addRight(elementLeft.right);
					
					r=newRoot;
					
				//(av(b^c)) to ((avb)^(avc))
				}else if(elementRight.value.equals("^")) {
					Node newRoot = new Node("^");
					//left of root
					newRoot.left = new Node("v", newRoot);
					newRoot.left.addLeft(r.left);
					newRoot.left.addRight(elementRight.left);
					//right of root
					newRoot.right = new Node("v", newRoot);
					newRoot.right.addLeft(r.left);
					newRoot.right.addRight(elementRight.right);
					
					r=newRoot;
				}
			}
			
			//Used to remove parentheses
			else if(r.value.equals("()")) {
				r.right.parent = r.parent;
				r = r.right;
				
				r = convertToCnfForm(r);
				
				return r;
			}
			
			r.left = convertToCnfForm(r.left);
			r.right = convertToCnfForm(r.right);
			return r;
		}
		
		
		/*
		 * PRE: Call this on a ParseTree once it has been converted to CNF form. Call convertToCnfForm() before calling this method
		 * 
		 * Returns the String representation of the sentence(CNF form)
		 * 
		 * Parameter(s): None
		 * 
		 * Returns: String
		 */
		private String toStr() { 
			String retStr = "";
			retStr = toStr(root, retStr);
			if(unarySentance)
				retStr = "("+retStr+")";
			return "Result: " + retStr;
		}
		
		/*
		 * PRE: Should be called through toStr()
		 * 
		 * Returns the String representation of the nodes descending from root n
		 * 
		 * Parameter(s): Node n, String retStr
		 * 
		 * Returns: String
		 */
		private String toStr(Node r, String retStr) {
			
			if(r == null)return retStr;
			
			String nodeVal = r.toStr();
			if(nodeVal.equals("()") || (nodeVal.equals(" ^ ") && !r.left.value.equals("^"))) retStr+="(";

			
			retStr = toStr(r.left, retStr);
			
			if(nodeVal.equals(" ^ ")) {
				if(!r.left.value.equals("^"))
					retStr+=")";
				unarySentance = false;
			}
			
			if(!nodeVal.equals("()"))
				retStr += nodeVal;
			
			if(nodeVal.equals(" ^ ") && !r.right.value.equals("^")) retStr+="(";
				
			retStr = toStr(r.right, retStr);
			
			if(nodeVal.equals("()") || (nodeVal.equals(" ^ ") && !r.right.value.equals("^"))) retStr+=")";
			
			return retStr;
		}
		
		/*
		 * The Node Class is used to apply an store the value along with a left(child), right(child) and a parent of the node within a tree
		 * 
		 * Attributes: Node root, static boolean parsePrint, boolean unarySentance
		 * 
		 * Constructor: Node(String v), Node(String v, Node p)
		 * 
		 * Method: addLeft(Node n), addRight(Node n), toStr()
		 * 
		 * *Contains Class Node*
		 */
		private static class Node{
			//String value holds a string representation of the nodes value
			private String value = "";
			
			//Left child reference
			private Node left = null;
			//Right child reference
			private Node right = null;
			//Parent reference
			private Node parent = null;
			
			//Used to create a Node with value v with no references to other nodes
			Node(String v){
				value = v;
			}
			
			//Used to create a Node with value v with a reference to parent p
			Node(String v, Node p){
				value = v;
				parent = p;
			}
			
			/*
			 * Allows the node to add a left reference to Node n (node n will also make this its parent)
			 * 
			 * Parameter(s): Node n
			 * 
			 * Returns: None
			 */
			private void addLeft(Node n) {
				left = n;
				left.parent = this;
			}
			
			/*
			 * Allows the node to add a right reference to Node n (node n will also make this its parent)
			 * 
			 * Parameter(s): Node n
			 * 
			 * Returns: None
			 */
			private void addRight(Node n) {
				right = n;
				right.parent = this;
			}
			
			/*
			 * Returns the String representation of the node value
			 * 
			 * Parameter(s): None
			 * 
			 * Returns: String
			 */
			private String toStr() {
				if(value.equals("v")) return " v ";
				if(value.equals("^")) return " ^ ";
				return value;
			}
		}
	}
}
