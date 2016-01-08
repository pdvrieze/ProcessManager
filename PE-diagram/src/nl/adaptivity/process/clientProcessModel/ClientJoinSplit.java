package nl.adaptivity.process.clientProcessModel;


import nl.adaptivity.process.processModel.JoinSplit;


public interface ClientJoinSplit<T extends ClientProcessNode<T, M>, M extends ClientProcessModel<T,M>> extends JoinSplit<T, M>, ClientProcessNode<T, M> {

}