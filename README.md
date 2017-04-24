Word constraint tool, using ConceptNet and WordNet
===
Mark J. Nelson <http://www.kmjn.org>, 2007-2008, 2017

Define a generative space by constraints on interrelated sets of terms.
ConceptNet and WordNet provide background knowledge bases that can be used
to specify the constraints on these terms and their relationships. A solver
gives possible assignments to the terms that respect those constraints, and
an included GUI can be used to iteratively edit and refine the constraint
graph until you get the desired results.

This code was written for a project to auto-skin simple videogames (the terms
are mapped to game sprites), described in the following paper:
* Mark J. Nelson and Michael Mateas (2008). An interactive game-design
  assistant. In Proceedings of the 2008 International Conference on
  Intelligent User Interfaces, pages 90-98.
  http://www.kmjn.org/publications/Assistant_IUI08-abstract.html

Bundled dependencies:
* The [MIT Java Wordnet Interface](https://projects.csail.mit.edu/jwi/), version 2.4.0
* ConceptNet 3 by the MIT Common Sense Computing Initiative <http://conceptnet.media.mit.edu/>
* WordNet 3.1 by the Princeton Cognitive Science Laboratory <http://wordnet.princeton.edu/>
* JGraph by [JGraph Ltd.](http://www.jgraph.com/) Uses the old version now
  called [legacy JGraph](https://github.com/jgraph/legacy-jgraph5).

Overview
===

Each term (node in the GUI graph; variable in the constraint-satisfaction
problem) has a type, which for the moment is either 'noun' or 'verb'.  Each
also has a domain of possible values it can take on, before considering the
constraints; a solution is a selection of one of these values per term that
also respects all its constraints.

For example, we might say that a noun 'animal' can be one of 'dog, cat, parrot,
monkey, human, chicken'. The solver would then try to find one of these
possible animals that satisfies the ConceptNet and WordNet constraints we've
added to the 'animal' term. We can also specify a default list of possible
nouns and possible verbs, to use for terms where we don't want to explicitly
specify a list of possible values.

Aside: You might wonder why terms' domains don't default to all nouns and verbs
in WordNet as possibilities. One practical reason is because the constraint
solving, at least using the current mostly-brute-force algorithm, would be
intractable (although a quite large domain is fine). However, there are also
good reasons not to range over all possible words in the English language. In
the application this library was built for (auto-skinning videogames), nouns
could only be chosen from among those terms we had sprites for anyway, so this
was a natural restriction. Perhaps more importantly, ConceptNet is a fairly
messy database of commonsense knowledge, and it is far easier to produce a
usable constraint graph (perhaps using the included GUI editor to iteratively
refine it) if you're working in a smaller, closed domain of terms.

There can also be literal terms, which are equivalent to variable terms with
exactly one possible value in the domain (but using explicit literals makes the
constraint graph easier to read).

Constraints are required relationships between terms.

ConceptNet constraints require that some term have a ConceptNet LinkType
relationship to another one.  For example, we could require that the 'animal'
noun term be CapableOf an 'attack' verb term that ranges over some possible
types of attacks (shoot, chase, hit, injure, etc.), to get only attacking
animals.

WordNet constraints require that a term be a specialization (hyponym) or
generalization (hypernym) of another term. For example, we could restrict
the 'animal' noun term to animals that are specializations of 'mammal'.

We can combine the two kinds of constraints by using WordNet "inheritance" on
ConceptNet constraints, lifting ConceptNet relationships to classes of terms.
Instead of requiring that there be a ConceptNet relationship for a specific
term in order for it to be a legal assignment, we could allow the constraint to
be also satisfied if there is a relationship for a more general or more
specific version of the term (depending on what we want the constraint to
mean).

For example, if we required an 'animal' term to be CapableOf 'flying', this
might not match 'robin', because ConceptNet doesn't happen to know that robins
can fly. But it does know that birds are CapableOf flying, and from WordNet we
know that a robin is a specialization of a bird. So if we added "or more
general than" inheritance to the first term of the ConceptNet constraint, robin
would satisfy the constraint: a generalization of robin (bird) is CapableOf
flying.

Constraints can also be combined with the AND/OR/NOT boolean operators. By
default, all constraints are required, i.e. an implicit AND. If you add
an OR between two constraints, now only one of them is required. The explicit
AND between two constraints doesn't by itself do anything (since all constraints
are required already), but is there to allow you to refer to a conjunction of
two constraints as a single unit, e.g. to use it inside an OR for things
like "a OR (b AND c)".

Usage
===

The front-end tools are in gui.java and solver.java, compiling to gui.jar
and solver.jar respectively. The first is a GUI constraint-graph editor,
while the second takes an already specified constraint graph and produces
a list of solutions. An ant build file is provided.

Both require the ConceptNet datafile(s) and the WordNet data directory (both
included) to be in the working directory.

By default it uses a modified version of ConceptNet (included)with only terms
involving single-word entities, in conceptnet\_singlewords.txt. If you want to
use the original ConceptNet files, or other files, edit the FILENAMES constant
in ConceptNet.java.

GUI
---

There's a GUI editor for constraint graphs, though in parts it's a bit rough.
All editing is done through the right-click menu: right-click on any empty
space to add terms or literals, set default possibilities for nouns and verbs,
or solve for possible assignments. If a node is selected, you can also remove
it.  If two nodes are selected, you can add constraints between them. If a
constraint is selected, you can remove or negate it. If two constraints are
selected, you can AND or OR them.

The middle-click menu lets you save or load constraint graphs. Saving generates
two files: the filename you enter contains the semantic information in the
constraint graph, and a secondary filename.graph file saves the geometric
layout. These files are reasonably human-readable, and can be edited, but
the loader isn't robust at all so that should only be done if you understand
the format. The safest edits to make to the file are things like changing
a list of possibile values for a term.

The GUI needs work on editability, e.g. being able to edit a term node without
removing and re-adding it, or being able to edit the list of default possible
values without re-entering it. In the meantime editing the save file for these
types of edits and then reloading is probably the quickest method.

The explanations of why a particular assignment did or didn't match are also
not fully implemented. For that and more, see the TODO comments in gui.java.

Backend and libraries
---

The solver is implemented in ConstraintSolver.java, and runs a fairly
straightforward brute-force algorithm, with minor optimizations to avoid
re-testing constraints after changes to variables that couldn't possibly affect
them. Constraints implement the Constraint interface. Variables are the
Variable class.

From the backend's perspective, a Variable has a name, a type, and optionally a
list of possible assignments (otherwise it defaults to the list for its type,
or no valid assignments if no default list is specified for that type).
Specific constraints may use a variable's type to determine how they operate,
e.g. the ones relating to WordNet currently assume only "noun" and "verb"
types, and look up terms accordingly. Other types can be used arbitrarily as
far as the solver (and saving/loading) is concerned, as long as they're only
used with constraints that can operate on that type of variable. New constraints
can be added, but in addition to implementing the Constraint interface, they
need to have an appropriate hook added to ConstraintFactory.newConstraint() to
re-instantiate them from whatever format their fullString() serializer
produces.

ConceptNet.java loads ConceptNet into memory with some indexed data structures,
and provides a number of utilities to query it. It might be useful by itself
for other code, though note that it depends on WordNet as well due to
implementing the inheritance/lifting features.

WordNet.java implements utility functions for finding/testing hypernyms, word
distance, etc., implemented on top of the JWI library.
