package src.simpleTests;

import java.util.Comparator;
import java.util.Iterator;

import org.jgrapht.DirectedGraph;
import org.jgrapht.GraphMapping;
import org.jgrapht.alg.isomorphism.VF2GraphIsomorphismInspector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;

import src.utilities.DefaultLabeledEdge;
import src.utilities.PatternNode;

class Obj {
	public String lbl;
	public int id;

	public Obj(String lbl) {
		this.lbl = lbl;
		// this.id = id;

	}

	@Override
	public String toString() {
		return lbl.toString();
	}
}

public class JGraphTester {

	public static void main(String[] args) {
		Obj obja = new Obj("a");
		Obj objb = new Obj("b");
		Obj objc = new Obj("c");
		Obj objd = new Obj("d");
		Obj obje = new Obj("e");

		SimpleGraph<Obj, DefaultEdge> g1 = new SimpleGraph<>(DefaultEdge.class);

		g1.addVertex(obja);
		g1.addVertex(objb);
		g1.addVertex(objc);
		g1.addVertex(objd);
		g1.addVertex(obje);

		g1.addEdge(obja, objb);
		g1.addEdge(obja, objc);
		g1.addEdge(obja, obje);

		g1.addEdge(objb, objd);

		g1.addEdge(objc, objd);
		g1.addEdge(objc, obje);

		g1.addEdge(objd, obje);

		Obj obj1 = new Obj("1");
		Obj obj2 = new Obj("2");
		Obj obj3 = new Obj("3");
		Obj obj4 = new Obj("4");
		Obj obj5 = new Obj("5");

		SimpleGraph<Obj, DefaultEdge> g2 = new SimpleGraph<>(DefaultEdge.class);

		g2.addVertex(obj1);
		g2.addVertex(obj2);
		g2.addVertex(obj3);
		g2.addVertex(obj4);
		g2.addVertex(obj5);

		g2.addEdge(obj1, obj4);
		g2.addEdge(obj1, obj2);
		g2.addEdge(obj1, obj5);

		g2.addEdge(obj2, obj3);
		g2.addEdge(obj2, obj5);

		g2.addEdge(obj3, obj4);
		g2.addEdge(obj3, obj5);

		VF2GraphIsomorphismInspector<Obj, DefaultEdge> vf2Iso = new VF2GraphIsomorphismInspector<Obj, DefaultEdge>(g1,
				g2);

		System.out.println(vf2Iso.isomorphismExists());

		Iterator<GraphMapping<Obj, DefaultEdge>> mappingItr = vf2Iso.getMappings();

		while (mappingItr.hasNext()) {

			System.out.println(mappingItr.next());
			;
		}
		// VF2GraphMappingIterator<Obj, DefaultEdge>
		// vf2Iso.getMappings();

	}

}
