# Propositional Logic Proofs (AI Project 3)

## Introduction

This project uses clauses, or sentences (converted into clauses) to prove other statements.

Before you use this project it is important that you have a brief understanding of some basics of Propositional Logic (PL).

If you already know PL then the help command is the only resourse necessary to understand how to use the program.

## PL Basics

Connectives: ~(not), v(or), ^(and), =>(implies), <=>(if and only if)  
NOTE: Parentheses can also be used for ordering

Conjuctive Normal Form Phrases(CNF):  
Symbol  : A | B | C | ... (any other word or letter) NOTE: lowercase v can not be used for "or" disjunctions  
Literal : Symbol | ~Symbol (either a Symbol or a negated Symbol)  
Clause  : Literal | Literal v Literal | Literal v Literal v Literal | ... (disjunction(only "or" connectives) of literals)  
Sentence: (Clause) | (Clause) ^ (Clause) | ... (conjunction(only "and" connectives) of clauses)   

NOTE: If you try to use a tellc command on a statement that is not a clause it will break this program. If you are telling a sentence please use tell


###### Author: Jackson Mishuk
