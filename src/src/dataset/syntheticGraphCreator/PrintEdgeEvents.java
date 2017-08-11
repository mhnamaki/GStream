package src.dataset.syntheticGraphCreator;
import org.graphstream.algorithm.generator.BarabasiAlbertGenerator;
import org.graphstream.algorithm.generator.Generator;
import org.graphstream.stream.Sink;
import org.graphstream.stream.SinkAdapter;

/**
 * Created by pigne on 8/11/16.
 */
public class PrintEdgeEvents {
    public static void main(String[] args) {
        int maxEdgePerEvent = 5;
        int numberOfNodes = 10;
        
        Generator generator = new BarabasiAlbertGenerator(maxEdgePerEvent);

        Sink s = new SinkAdapter(){
            @Override
            public void edgeAdded(String sourceId, long timeId, String edgeId, String fromNodeId, String toNodeId, boolean directed) {
                System.out.printf("Edge %s added from node %s to node %s%n", edgeId,fromNodeId, toNodeId);
            }
        };

        generator.addSink(s);
        generator.begin();

        for (int i = 0; i < numberOfNodes; i++) {
            generator.nextEvents(); 
        }
    }
}