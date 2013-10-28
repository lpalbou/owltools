package owltools.sim2;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;

import com.google.common.collect.Sets;
import com.googlecode.javaewah.EWAHCompressedBitmap;

import owltools.OWLToolsTestBasics;
import owltools.io.ParserWrapper;
import owltools.sim2.preprocessor.ABoxUtils;

public class SimSpeedTest extends OWLToolsTestBasics{

	OWLOntology ontology;
	OWLReasonerFactory reasonerFactory;
	OWLReasoner reasoner;
	Map<OWLClass,Set<Node<OWLClass>>> superclassMap;
	Map<OWLClass,Set<Integer>> superclassIntMap;
	Map<OWLClass,EWAHCompressedBitmap> superclassBitmapMap;
	Map<OWLClass,Integer> classIndex;
	Vector<OWLClass> classByNumber;
	OWLClass[] classArray;

	public enum Method {NAIVE, INTS, GUAVA, EFFICIENT, BITMAP};

	@Test
	public void testNaiveWorm() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.NAIVE, "wbphenotype", 2000);
	}
	@Test
	public void testNaiveMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.NAIVE, "mp", 2000);
	}
	@Test
	public void testEfficientMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.EFFICIENT, "mp", 2000);
	}
	@Test
	public void testIntsMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.INTS, "mp", 2000);
	}
	@Test
	public void testGuavaIntsMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.GUAVA, "mp", 2000);
	}
	@Test
	public void testBitmapMouse() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.BITMAP, "mp", 2000);
	}
	@Test
	public void testNaiveUpheno() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.NAIVE, "upheno", 2000);
	}
	@Test
	public void testIntsUpheno() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.INTS, "upheno", 2000);
	}
	@Test
	public void testGuavaIntsUpheno() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.GUAVA, "upheno", 2000);
	}
	@Test
	public void testBitmapUpheno() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.BITMAP, "upheno", 2000);
	}
	@Test
	public void testNaiveMammal() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.NAIVE, "mammal-merged", 2000);
	}
	@Test
	public void testGuavaIntsMammal() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.GUAVA, "mammal-merged", 2000);
	}
	@Test
	public void testBitmapMammal() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.BITMAP, "mammal-merged", 2000);
	}
	@Test
	public void testBitmapMammal10k() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		t(Method.BITMAP, "mammal-merged", 10000);
	}
	@Test
	public void bitmapTest() throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		Set<Integer> ixs = new HashSet<Integer>();
		ixs.add(99);
		ixs.add(2000);
		ixs.add(7777);
		EWAHCompressedBitmap bm = bm(ixs);
		Set<Integer> ixs2 = new HashSet<Integer>();
		for (int i : bm.toArray()) {
			System.out.println(i);
			ixs.add(i);
		}
		assert(ixs.equals(ixs2));
		
	}

	public void t(Method m, String idspace, int size) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		Runtime rt = Runtime.getRuntime();
		load(idspace);
		ABoxUtils.makeDefaultIndividuals(ontology);
		long usedMB0 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		precompute(m);
		long t1 = System.currentTimeMillis();
		long usedMB1 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("LOADED: "+idspace+" M:"+m+" NUM:"+size+" usedMB:"+usedMB0);
		msg("TEST: "+idspace+" M:"+m+" NUM:"+size+" usedMB:"+usedMB1);
		axa(m, size);
		long t2 = System.currentTimeMillis();
		long usedMB2 = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
		msg("COMPLETED TEST: "+idspace+" M:"+m+" NUM:"+size+" usedMB:"+usedMB2+
				" usedMBDelta:" + (usedMB2-usedMB1) +
				" TIME (ms):"+(t2-t1));		

		if (idspace.equals("mp")) {
			OWLClass c1 = getCls("MP_0003737"); // ossification of pinnae
			OWLClass c2 = getCls("MP_0002895"); // abnormal otolithic membrane morphology
			assert(1 == jaccard(c1, c1, m));
			assert(1 == jaccard(c2, c2, m));
			msg("SIMTEST="+jaccard(c1,c2,m));
			Set<Node<OWLClass>> c1ancs = ancsByMethod(c1,m);
			msg("ANCS(c1) = "+c1ancs.size()+" // "+c1ancs);
		}
	}

	private OWLClass getCls(String id) {
		return 
				ontology.getOWLOntologyManager().getOWLDataFactory().
				getOWLClass(IRI.create("http://purl.obolibrary.org/obo/"+id));
	}
	private void precompute(Method m) {
		msg("Precomputing...");

		Set<OWLClass> cset = ontology.getClassesInSignature();

		int n=0;
		classByNumber = new Vector<OWLClass>(cset.size());
		classArray = (OWLClass[]) Array.newInstance(OWLClass.class, cset.size());
		classIndex = new HashMap<OWLClass,Integer>();
		for (OWLClass c : cset) {
			ancsCachedModifiable(c);
			classArray[n] = c;
			classIndex.put(c, n);
			//classByNumber.add(index, element);
			n++;
		}

		for (OWLClass c : cset) {
			ancsCachedModifiable(c);
			ancsIntsCachedModifiable(c);
			ancsBitmapCachedModifiable(c);
		}


		msg("Precomputed DONE");

	}

	private void axa(Method m, int size) {
		Set<OWLClass> all = ontology.getClassesInSignature();
		Set<OWLClass> cset = new HashSet<OWLClass>();
		int i=0;
		for (OWLClass c : ontology.getClassesInSignature()) {
			i++;
			if (i>size)
				break;
			cset.add(c);
		}
		axa(cset, cset, m);
	}

	public void axa(Set<OWLClass> cset, Set<OWLClass> dset, Method m) {
		if (m.equals(Method.EFFICIENT))
			axaEfficient(cset, dset);
		double total = 0;
		int n = 0;
		for (OWLClass c : cset) {
			//msg("Attr(c)="+c);
			for (OWLClass d : cset) {
				double s = jaccard(c,d,m);
				//msg(" "+c+" , "+d+" = "+s);
				total += s;
				n++;
			}
		}
		msg("AVG: "+total / (double) n);
	}

	public void axaEfficient(Set<OWLClass> cset, Set<OWLClass> dset) {
		for (OWLClass c : cset) {
			Set<Node<OWLClass>> ca = ancsCachedModifiable(c);

			//msg("Attr(c)="+c);
			for (OWLClass d : cset) {
				Set<Node<OWLClass>> cd = ancsCachedModifiable(d);
				Set<Node<OWLClass>> cad = new HashSet<Node<OWLClass>>(ca);
				cad.retainAll(cd);
				Set<Node<OWLClass>> cud = new HashSet<Node<OWLClass>>(ca);
				cud.addAll(cd);
				double s = cad.size() / (float) cud.size();
				//msg(" "+c+" , "+d+" = "+s);
			}
		}
	}


	public double jaccard(OWLClass c, OWLClass d, Method m) {
		if (m.equals(Method.NAIVE))
			return jaccardNaive(c,d);
		else if (m.equals(Method.BITMAP))
			return jaccardBitmap(c,d);
		else if (m.equals(Method.INTS))
			return jaccardInts(c,d);
		else if (m.equals(Method.GUAVA))
			return jaccardGuavaInts(c,d);
		else return -1;

	}



	public double jaccardNaive(OWLClass c, OWLClass d) {
		Set<Node<OWLClass>> ca = ancsCachedModifiable(c);
		Set<Node<OWLClass>> cd = ancsCachedModifiable(d);
		Set<Node<OWLClass>> cad = new HashSet<Node<OWLClass>>(ca);
		cad.retainAll(cd);
		Set<Node<OWLClass>> cud = new HashSet<Node<OWLClass>>(ca);
		cud.addAll(cd);
		return cad.size() / (double) cud.size();
	}

	public double jaccardInts(OWLClass c, OWLClass d) {
		Set<Integer> ca = ancsIntsCachedModifiable(c);
		Set<Integer> cd = ancsIntsCachedModifiable(d);
		Set<Integer> cad = new HashSet<Integer>(ca);
		cad.retainAll(cd);
		Set<Integer> cud = new HashSet<Integer>(ca);
		cud.addAll(cd);
		return cad.size() / (double) cud.size();
	}

	public double jaccardGuavaInts(OWLClass c, OWLClass d) {
		Set<Integer> ca = ancsIntsCachedModifiable(c);
		Set<Integer> cd = ancsIntsCachedModifiable(d);
		Set<Integer> cad = Sets.intersection(ca	, cd);
		Set<Integer> cud = Sets.union(ca, cd);
		return cad.size() / (double) cud.size();
	}

	private double jaccardBitmap(OWLClass c, OWLClass d) {
		EWAHCompressedBitmap bmc = ancsBitmapCachedModifiable(c);
		EWAHCompressedBitmap bmd = ancsBitmapCachedModifiable(d);

		return bmc.andCardinality(bmd) / (double) bmc.orCardinality(bmd);
	}


	private EWAHCompressedBitmap bm(Set<Integer> bits) {
		EWAHCompressedBitmap bm = new EWAHCompressedBitmap();
		ArrayList<Integer> bitlist = new ArrayList<Integer>(bits);
		Collections.sort(bitlist);
		for (Integer i : bitlist) {
			bm.set(i.intValue());
		}
		return bm;
	}

	public EWAHCompressedBitmap ancsBitmapCachedModifiable(OWLClass c) {
		if (superclassBitmapMap != null && superclassBitmapMap.containsKey(c)) {
			return superclassBitmapMap.get(c);
		}
		Set<Integer> caints = ancsIntsCachedModifiable(c);
		//msg(c + " ancs = "+caints.size());
		EWAHCompressedBitmap bm = bm(caints);
		if (superclassBitmapMap == null)
			superclassBitmapMap = new HashMap<OWLClass,EWAHCompressedBitmap>();
		superclassBitmapMap.put(c, bm);
		return bm;		
	}


	public Set<Integer> ancsIntsCachedModifiable(OWLClass c) {
		if (superclassIntMap != null && superclassIntMap.containsKey(c)) {
			return superclassIntMap.get(c);
		}
		Set<Integer> a = ancsInts(c);
		if (superclassIntMap == null)
			superclassIntMap = new HashMap<OWLClass,Set<Integer>>();
		superclassIntMap.put(c, a);
		return a;
	}	


	public Set<Integer> ancsInts(OWLClass c) {
		Set<Node<OWLClass>> ancs = ancsCachedModifiable(c);
		Set<Integer> ancsInts = new HashSet<Integer>();
		OWLClass thing = owlThing();
		for (Node<OWLClass> anc : ancs) {
			// TODO - verify robust for non-Rep elements
			OWLClass ac = anc.getRepresentativeElement();
			if (ac.equals(thing))
				continue;
			Integer ix = classIndex.get(ac);
			if (ix == null) {
				msg("??"+anc);
			}
			ancsInts.add(ix.intValue());
		}
		return ancsInts;
	}
	public Set<Node<OWLClass>> ancsCached(OWLClass c) {
		return new HashSet<Node<OWLClass>>(ancsCachedModifiable(c));
	}
	public Set<Node<OWLClass>> ancsCachedModifiable(OWLClass c) {
		if (superclassMap != null && superclassMap.containsKey(c)) {
			return superclassMap.get(c);
		}
		Set<Node<OWLClass>> a = ancs(c);
		if (superclassMap == null)
			superclassMap = new HashMap<OWLClass,Set<Node<OWLClass>>>();
		superclassMap.put(c, a);
		return a;
	}	
	public Set<Node<OWLClass>> ancs(OWLClass c) {
		NodeSet<OWLClass> ancs = reasoner.getSuperClasses(c, false);
		Set<Node<OWLClass>> nodes = ancs.getNodes();
		nodes.add(reasoner.getEquivalentClasses(c));
		nodes.remove(owlThingNode());
		return nodes;
	}

	public Set<Node<OWLClass>> ancsByMethod(OWLClass c, Method m) {
		if (m.equals(Method.NAIVE)) {
			return ancs(c);
		}
		if (m.equals(Method.INTS) || m.equals(Method.GUAVA)) {
			Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
			for (Integer ix : this.ancsIntsCachedModifiable(c)) {
				OWLClassNode node = new OWLClassNode(classArray[ix]);
				nodes.add(node);
			}
			return nodes;
		}
		if (m.equals(Method.BITMAP)) {
			Set<Node<OWLClass>> nodes = new HashSet<Node<OWLClass>>();
			EWAHCompressedBitmap bm = ancsBitmapCachedModifiable(c);
			for (int ix : bm.toArray()) {
				OWLClassNode node = new OWLClassNode(classArray[ix]);
				nodes.add(node);
			}
			return nodes;
		}
		NodeSet<OWLClass> ancs = reasoner.getSuperClasses(c, false);
		Set<Node<OWLClass>> nodes = ancs.getNodes();
		nodes.add(reasoner.getEquivalentClasses(c));
		return nodes;
	}

	private void load(String idspace) throws OWLOntologyCreationException, OBOFormatParserException, IOException {
		msg("Loading");
		ParserWrapper pw = new ParserWrapper();
		ontology = pw.parseOBO("/Users/cjm/repos/phenotype-ontologies/src/ontology/"+idspace+".obo");
		reasonerFactory = new ElkReasonerFactory();
		reasoner = reasonerFactory.createReasoner(ontology);
		msg("Loaded. Root = " + reasoner.getRootOntology());
	}

	private void msg(String s) {
		System.out.println(s);
	}

	OWLClass thing = null;
	Node<OWLClass> thingNode = null;
	public OWLClass owlThing() {
		if (thing == null)
			thing = ontology.getOWLOntologyManager().getOWLDataFactory().getOWLThing();
		return thing;
	}
	public Node<OWLClass> owlThingNode() {
		if (thingNode == null)
			thingNode = reasoner.getTopClassNode();
		return thingNode;
	}

}
