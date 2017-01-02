/*
 * Copyright (c) 2016.
 *
 * This file is part of ProcessManager.
 *
 * ProcessManager is free software: you can redistribute it and/or modify it under the terms of version 3 of the
 * GNU Lesser General Public License as published by the Free Software Foundation.
 *
 * ProcessManager is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with ProcessManager.  If not,
 * see <http://www.gnu.org/licenses/>.
 */

/*
 * Created on Oct 15, 2004
 */

package net.devrieze.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;


/**
 * This class implements a thread that writes everything in an
 * {@link InputStream}to an {@link OutputStream}. The thread is automatically
 * stopped when the InputStream is closed. The main function of this class is
 * for implementing pipes between external commands.
 * 
 * @author Paul de Vrieze
 * @version 1.0 $Revision$
 */
public final class InputStreamOutputStream implements Callable<Boolean> {

  private static final int _BUFFERSIZE = 4048;

  /**
   * The InputStream that must be read.
   */
  private final InputStream mInputStream;

  /**
   * The OutputStream that must be written to.
   */
  private final OutputStream mOutputStream;

  /**
   * Create a new InputStreamOutputStream. This class is private to ensure that
   * the {@link #getInputStreamOutputStream(InputStream, OutputStream)}factory
   * method is used. That is needed as the constructor can not bring the class
   * in the desired consistent state.
   * 
   * @param pInputStream The InputStream to read from.
   * @param pOutputStream The OutputStream to write to.
   */
  private InputStreamOutputStream(final InputStream pInputStream, final OutputStream pOutputStream) {
    super();
    mInputStream = pInputStream;
    mOutputStream = pOutputStream;
  }

  /**
   * This static factory method will create an InputStreamOutputStream and start
   * it.
   * 
   * @param pInputStream The stream to read from.
   * @param pOutputStream The stream to write to.
   * @return A {@link Future}object that will return {@link Boolean#TRUE}when no
   *         errors occurred. Else the Future will contain an exception.
   */
  public static Future<Boolean> getInputStreamOutputStream(final InputStream pInputStream, final OutputStream pOutputStream) {
    DebugTool.ensureParamNotNull(pInputStream);
    DebugTool.ensureParamNotNull(pOutputStream);

    final InputStreamOutputStream s = new InputStreamOutputStream(pInputStream, pOutputStream);
    final FutureTask<Boolean> f = new FutureTask<>(s);
    new Thread(f).start();

    return f;
  }

  public static void writeToOutputStream(final InputStream pInputStream, final OutputStream pOutputStream) throws IOException {
    DebugTool.ensureParamNotNull(pInputStream);
    DebugTool.ensureParamNotNull(pOutputStream);
    final InputStreamOutputStream s = new InputStreamOutputStream(pInputStream, pOutputStream);
    s.call();
  }

  /**
   * The call function for this object. This function needs to be public for the
   * object to be callable. The private nature of the constructor should ensure
   * that it can not actually be used except through the factory method.
   * 
   * @return {@link Boolean#TRUE}if nothing went wrong, else it throws an
   *         exception.
   * @throws IOException if something went wrong in the reading and writing
   */
  @Override
  public Boolean call() throws IOException {
    final byte[] buffer = new byte[_BUFFERSIZE];
    int read;
    try {
      while ((read = mInputStream.read(buffer)) >= 0) {
        mOutputStream.write(buffer, 0, read);
      }
    } finally {
      mInputStream.close();
      mOutputStream.close();
    }
    return Boolean.TRUE;
  }

}
