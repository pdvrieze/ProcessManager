package nl.adaptivity.process.engine.processModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import nl.adaptivity.process.engine.ProcessModel;

@Deprecated
public class ProcessModelXmlAdapter extends XmlAdapter<XmlProcessModel, ProcessModel> {

  @Override
  public XmlProcessModel marshal(ProcessModel pProcessModel) throws Exception {
    XmlProcessModel result = new XmlProcessModel();
    List<ProcessNode> list = result.getNodes();
    HashSet<String> seen = new HashSet<String>();
    for(StartNode node: pProcessModel.getStartNodes()) {
      extractElements(list, seen, node);
    }
    return result;
  }

  private static void extractElements(List<ProcessNode> pTo, HashSet<String> pSeen, ProcessNode pNode) {
    if (pSeen.contains(pNode.getId())) {
      return;
    }
    pTo.add(pNode);
    pSeen.add(pNode.getId());
    for(ProcessNode node:pNode.getSuccessors()) {
      extractElements(pTo, pSeen, node);
    }
  }

  @Override
  public ProcessModel unmarshal(XmlProcessModel pModel) throws Exception {
    List<EndNode> result = new ArrayList<EndNode>();
    
    for(ProcessNode node: pModel.getNodes()) {
      if (node instanceof EndNode) {
        result.add((EndNode) node);
      }
    }
    return new ProcessModel(result);
  }

}
