package uk.ac.bournemouth.darwin.catalina.realm;


public abstract class DarwinBasePrincipal implements DarwinPrincipal {

  private static final long MAX_CACHE=60000; // One minute cache
  
  private String aName;
  private long aLastChecked=Long.MIN_VALUE;

  @Override
  public String getName() {
    return aName;
  }

  
  public DarwinBasePrincipal(String pName) {
    aName = pName;
  }

  protected boolean needsRefresh() {
    final long now = System.currentTimeMillis();
    return now < aLastChecked+MAX_CACHE;
  }

}
