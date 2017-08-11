package src.dataset.syntheticGraphCreator;

/**
 * Used for SyntheticFreeScaleGraphGenerator
 * Created by shayan on 7/12/16.
 */
public class Edge {
   // int sourceId;
    int addTimeStamp;
    int deleteTimeStamp;
    String relationshipType;

    public Edge()
    {

    }


    public void setTimeStamps(int add, int delete){
        this.deleteTimeStamp = delete;
        this.addTimeStamp = add;
    }


    public int getAddTimeStamp(){
        return addTimeStamp;
    }
    public int getDeleteTimeStamp(){
        return deleteTimeStamp;
    }

}
