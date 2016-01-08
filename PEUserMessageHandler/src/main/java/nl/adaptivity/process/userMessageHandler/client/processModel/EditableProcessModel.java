package nl.adaptivity.process.userMessageHandler.client.processModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class EditableProcessModel {

  private final ProcessModel mProcessModel;

  List<EditableProcessNode> mNodes;

  public EditableProcessModel(final ProcessModel processModel) {
    mProcessModel = processModel;
  }

  public Collection<EditableProcessNode> getNodes() {
    if (mNodes == null) {
      final List<ProcessNode> nodes = mProcessModel.getNodes();
      mNodes = new ArrayList<EditableProcessNode>(nodes.size());
      for (final ProcessNode node : nodes) {
        mNodes.add(EditableProcessNode.create(node));
      }
    }
    return mNodes;
  }

  public void layout() {
    mProcessModel.layout();
    // TODO layout nodes
  }

}
