package nl.adaptivity.process.processModel;



public interface Join<T extends ProcessNode<T>> extends ProcessNode<T>, JoinSplit<T> {
  // No methods beyond JoinSplit
}