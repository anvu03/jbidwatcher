package com.jbidwatcher.ui;
/*
 * Copyright (c) 2000-2007, CyberFOX Software, Inc. All Rights Reserved.
 *
 * Developed by mrs (Morgan Schweers)
 */

import com.jbidwatcher.util.config.JConfig;
import com.jbidwatcher.util.config.ErrorManagement;

import java.io.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.lang.reflect.*;

public class JDropListener implements DropTargetListener {
  private JDropHandler handler;

  private boolean _windows = false;
  private DataFlavor _isoFlavor = null;
  private DataFlavor _ascFlavor = null;
  private DataFlavor _plainFlavor = null;
  private DataFlavor _utf8HtmlFlavor = null;
  private DataFlavor _thtmlFlavor = null;
  private DataFlavor _pl2Flavor = null;
  private DataFlavor _htmlFlavor = null;
  private DataFlavor _urlFlavor = null;

  private static final String[][] _str_flavors = {
    { "UTF8Html", "text/html; class=java.io.InputStream; charset=utf-8" },
    { "isoFlavor", "text/plain; class=java.io.InputStream; charset=iso8859-1" },
    { "ascFlavor", "text/plain; class=java.io.InputStream; charset=ascii" },
    { "pl2Flavor", "text/plain; class=java.io.InputStream" },
    { "thtmlFlavor", "text/html" },
    { "htmlFlavor", "text/html; class=java.io.Reader; charset=Unicode" },
    { "urlFlavor", "application/x-url; class=java.io.InputStream" }
  };

  private static final int ALL_ACTIONS = DnDConstants.ACTION_COPY_OR_MOVE | DnDConstants.ACTION_LINK;

  public JDropListener(JDropHandler inHandler) {
    super();

    setupFlavors();
    handler = inHandler;
  }

  private DataFlavor getDataFlavor(DataFlavor inFlavor, String whichFlavor) {
    DataFlavor df;
    int i;

    if(inFlavor != null) return inFlavor;

    for(i=0; i<_str_flavors.length; i++) {
      if(whichFlavor.equals(_str_flavors[i][0])) {
        try {
          df = new DataFlavor(_str_flavors[i][1]);
        } catch(ClassNotFoundException e) {
          ErrorManagement.logDebug("Failed to initialize " + whichFlavor);
          df = null;
        }
        return df;
      }
    }
    return null;
  }

  private void setupFlavors() {
    //  If it's NOT Windows
	  _windows = System.getProperty("os.name").indexOf("indows") != -1;

    //  Deprecated, generally unused, but deprecation didn't provide
    // a useful alternative w/o rewriting a lot of code, so I'm keeping it for now.
    if(_plainFlavor == null) {
//      _plainFlavor = DataFlavor.plainTextFlavor;
      _plainFlavor = DataFlavor.getTextPlainUnicodeFlavor();
    }

    _isoFlavor = getDataFlavor(_isoFlavor, "isoFlavor");
    _ascFlavor = getDataFlavor(_ascFlavor, "ascFlavor");
    _pl2Flavor = getDataFlavor(_pl2Flavor, "pl2Flavor");
    _htmlFlavor= getDataFlavor(_htmlFlavor,"htmlFlavor");
    _utf8HtmlFlavor = getDataFlavor(_htmlFlavor,"UTF8Html");
    _thtmlFlavor = getDataFlavor(_htmlFlavor,"thtmlFlavor");
    _urlFlavor = getDataFlavor(_urlFlavor, "urlFlavor");
  }

  private void dumpDataFlavors(DataFlavor[] dfa) {
    int j;

    if(dfa != null) {
      if(dfa.length == 0) {
        System.err.println("Length is still zero!");
      }
      for(j=0; j<dfa.length; j++) {
        System.err.println("Flavah " + j + " == " + dfa[j].getHumanPresentableName());
        System.err.println("Flavah/mime " + j + " == " + dfa[j].getMimeType());
      }
    } else {
      System.err.println("Flavahs supported: none!\n");
    }
  }

  private boolean testFlavor(DataFlavor inFlavor, Transferable t) {
    if(inFlavor != null) {
      if(t.isDataFlavorSupported(inFlavor)) {
        ErrorManagement.logVerboseDebug("Accepting(2): " + inFlavor.getMimeType());
        return true;
      }
    }
    return false;
  }

  private boolean testFlavor(DataFlavor inFlavor, DropTargetDragEvent t) {
    if(inFlavor != null) {
      if(t.isDataFlavorSupported(inFlavor)) {
        /*
         * I think this has been debugged enough.  This gets annoying.
         */
        ErrorManagement.logVerboseDebug("Accepting(1): " + inFlavor.getMimeType());
        return true;
      }
    }
    return false;
  }

  private DataFlavor testAllFlavors(Transferable t) {
    if(testFlavor(_htmlFlavor, t)) return _htmlFlavor;
    if(testFlavor(_utf8HtmlFlavor, t)) return _utf8HtmlFlavor;
    if(testFlavor(_thtmlFlavor, t)) return _thtmlFlavor;

    if(testFlavor(_urlFlavor, t)) return _urlFlavor;
    if(_windows && testFlavor(_ascFlavor, t)) return _ascFlavor;

    if(testFlavor(_isoFlavor, t)) return _isoFlavor;
    if(testFlavor(_plainFlavor, t)) return _plainFlavor;
    if(testFlavor(_pl2Flavor, t)) return _pl2Flavor;

    if(testFlavor(DataFlavor.stringFlavor, t)) return DataFlavor.stringFlavor;

    return null;
  }

  private DataFlavor testAllFlavors(DropTargetDragEvent dtde) {
    if(testFlavor(_htmlFlavor, dtde)) return _htmlFlavor;
    if(testFlavor(_utf8HtmlFlavor, dtde)) return _utf8HtmlFlavor;
    if(testFlavor(_thtmlFlavor, dtde)) return _thtmlFlavor;

    if(testFlavor(_urlFlavor, dtde)) return _urlFlavor;
    if(_windows && testFlavor(_ascFlavor, dtde)) return _ascFlavor;

    if(testFlavor(_isoFlavor, dtde)) return _isoFlavor;
    if(testFlavor(_plainFlavor, dtde)) return _plainFlavor;
    if(testFlavor(_pl2Flavor, dtde)) return _pl2Flavor;

    if(testFlavor(DataFlavor.stringFlavor, dtde)) return DataFlavor.stringFlavor;

    return null;
  }

  private void acceptDrag(DropTargetDragEvent dtde) {
    int dragaction = dtde.getDropAction();

    if(dragaction != 0) {
      dtde.acceptDrag(dragaction);
    } else {
      dtde.acceptDrag(ALL_ACTIONS);
    }
  }

  private void acceptDrop(DropTargetDropEvent dtde) {
    int dragaction = dtde.getDropAction();

    if(dragaction != 0) {
      dtde.acceptDrop(dragaction);
    } else {
      dtde.acceptDrop(ALL_ACTIONS);
    }
  }

  private void checkDrag(DropTargetDragEvent dtde) {
    int da = dtde.getDropAction();
    if(dtde.getCurrentDataFlavors().length == 0) {
      ErrorManagement.logVerboseDebug("Zero length accepted... (" + da + ")");
      acceptDrag(dtde);
      return;
    }
    if(testAllFlavors(dtde) != null) {
      ErrorManagement.logVerboseDebug("Accepting drag! (" + da + ")");
      acceptDrag(dtde);
    } else {
      dtde.rejectDrag();
      ErrorManagement.logVerboseDebug("Rejecting drag! (" + da + ")");
    }
  }

  public void dragEnter(DropTargetDragEvent dtde) {
    ErrorManagement.logVerboseDebug("DragEnter!");
    checkDrag(dtde);
    if(JConfig.queryConfiguration("debug.uber", "false").equals("true") && JConfig.debugging) dumpDataFlavors(dtde.getCurrentDataFlavors());
  }

  public void dragOver(DropTargetDragEvent dtde) {
    checkDrag(dtde);
  }

  public void dragExit(DropTargetEvent dtde) { ErrorManagement.logVerboseDebug("Drag exited!"); }
  public void dropActionChanged(DropTargetDragEvent dtde) {
    acceptDrag(dtde);
    ErrorManagement.logVerboseDebug("Drag Action Changed!");
  }

  private void dumpFlavorsOld(Transferable t) {
    DataFlavor[] dfa = t.getTransferDataFlavors();

    if(dfa != null) {
      if(dfa.length == 0) {
        ErrorManagement.logVerboseDebug("Trying a second attack...");
        try {
          Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
          Transferable t2 = sysClip.getContents(null);
          StringBuffer stBuff;

          stBuff = getTransferData(t2);
          ErrorManagement.logVerboseDebug("Check out: " + stBuff);
        } catch(Exception e) {
          ErrorManagement.handleException("Caught: " + e, e);
        }
        ErrorManagement.logVerboseDebug("Done trying a second attack...");
      }
    }
    dumpDataFlavors(dfa);
  }

  private Method getSpecialAPI() {
    Class[] specialClasses = new Class[1];
    java.lang.reflect.Method acting;

    specialClasses[0] = Transferable.class;

    try {
      acting = DataFlavor.class.getMethod("getReaderForText", specialClasses);
    } catch(NoSuchMethodException nsme) {
      //  This is expected in earlier versions of the API!
      ErrorManagement.logDebug("No such method getReaderForText!");
      return null;
    }
    return acting;
  }

  private BufferedReader useNewAPI(Transferable t, DataFlavor dtf) {
    java.lang.reflect.Method acting;
    Object[] transfer = new Object[1];
    Reader dropReader = null;
    BufferedReader br;

    acting = getSpecialAPI();

    if(acting != null) {
      try {
        ErrorManagement.logVerboseDebug("Trying getReaderForText");

        //  This translates to:
        //          dropped = dtf.getReaderForText(t);
        //  Oddly enough, this appears to dump the text out to the
        //  console under win32-jre-1.4.0_01-b03
        transfer[0] = t;
        dropReader = (Reader)(acting.invoke(dtf, transfer));
      } catch(IllegalAccessException iae) {
        ErrorManagement.logDebug("Failed to invoke getReaderForText!  Illegal Access!");
      } catch(InvocationTargetException ite) {
        ErrorManagement.logDebug("Failed to invoke getReaderForText!  Bad Invocation Target!");
      }

      if(dropReader != null) {
        br = new BufferedReader(dropReader);
        return(br);
      }
    }
    return null;
  }

  private StringBuffer getDataFromReader(Reader br) {
    StringBuffer xferData = null;
    char[] buf = new char[513];
    int charsRead;

    try {
      do {
        charsRead = br.read(buf, 0, 512);
        if(charsRead != -1) {
          ErrorManagement.logVerboseDebug("Read: " + charsRead + " characters.");
          if(xferData == null) {
            xferData = new StringBuffer();
          }
          xferData.append(buf,0,charsRead);
        }
      } while(charsRead != -1);
      br.close();
    } catch(IOException e) {
      ErrorManagement.logDebug("Caught an IO Exception trying to read the drag/drop data!");
      return null;
    }

    return xferData;
  }

  private StringBuffer getDataFromStream(InputStream br) {
    return(getDataFromReader(new InputStreamReader(br)));
  }

  private StringBuffer getInputStreamData(Transferable t, DataFlavor dtf, InputStream dropStream) {
    StringBuffer xferData;
    BufferedReader br;

    br = useNewAPI(t, dtf);

    //  If the 'new' API failed...
    if(br == null) {
      if(JConfig.queryConfiguration("debug.uber", "false").equals("true")) ErrorManagement.logDebug("Non-getReaderForText: " + dropStream);
      try {
        InputStreamReader isr = new InputStreamReader(dropStream,"utf-16le");

        xferData = getDataFromStream(dropStream);
        if(xferData != null) {
          return xferData;
        } else {
          br = new BufferedReader(isr);
        }
      } catch(UnsupportedEncodingException uee) {
        ErrorManagement.logDebug("Unicode encoding unsupported.");
        br = new BufferedReader(new InputStreamReader(dropStream));
      }
    }

    xferData = getDataFromReader(br);

    return xferData;
  }

  public StringBuffer getTransferData(Transferable t) {
    StringBuffer xferData = null;
    Object dropped;
    DataFlavor dtf;

    dtf = testAllFlavors(t);

    ErrorManagement.logVerboseDebug("dtf == " + dtf);

    try {
      if(dtf == _htmlFlavor || dtf == _utf8HtmlFlavor || dtf == _thtmlFlavor) {
        /*
         * Annoying.
         */
        if(JConfig.queryConfiguration("debug.uber", "false").equals("true") && JConfig.debugging) System.out.println("Ick: " + t.getTransferData(DataFlavor.getTextPlainUnicodeFlavor()));
      }
      dropped = t.getTransferData(dtf);
    } catch(IOException ioe) {
      try { dropped = t.getTransferData(DataFlavor.stringFlavor); } catch(Exception e) {
        ErrorManagement.logDebug("I/O Exception: " + ioe);
        return null;
      }
    } catch(UnsupportedFlavorException ufe) {
      try { dropped = t.getTransferData(DataFlavor.stringFlavor); } catch(Exception e) {
        ErrorManagement.logDebug("Unsupported flavor: " + dtf);
        return null;
      }
    }

    if(dropped != null) {
      if(dropped instanceof InputStream) {
        ErrorManagement.logVerboseDebug("Dropped an InputStream");
        xferData = getInputStreamData(t, dtf, (InputStream)dropped);
      } else if(dropped instanceof Reader) {
        ErrorManagement.logVerboseDebug("Dropped a Reader");
        xferData = getDataFromReader(new BufferedReader((Reader)dropped));
      } else if(dropped instanceof java.net.URL) {
        ErrorManagement.logVerboseDebug("Dropped a URL");
        ErrorManagement.logVerboseDebug("Got: " + dropped.toString());

        xferData = new StringBuffer(dropped.toString());
      } else if(dropped instanceof String) {
        ErrorManagement.logVerboseDebug("Dropped a String");
        xferData = new StringBuffer((String)dropped);
      }

      return(xferData);
    }
    return null;
  }

  public void drop(DropTargetDropEvent dtde) {
    Transferable t = dtde.getTransferable();
    StringBuffer dropData=null;
    DataFlavor dtf;

    ErrorManagement.logVerboseDebug("Dropping!");

    if(t.getTransferDataFlavors().length == 0) {
      Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable t2 = sysClip.getContents(null);
      DataFlavor[] dfa2;
      int j;

      ErrorManagement.logDebug("Dropped 0 data flavors, trying clipboard.");
      dfa2 = null;

      if(t2 != null) {
        ErrorManagement.logVerboseDebug("t2 is not null: " + t2);
        dfa2 = t2.getTransferDataFlavors();
        ErrorManagement.logVerboseDebug("Back from getTransferDataFlavors()!");
      } else {
        ErrorManagement.logVerboseDebug("t2 is null!");
      }

      if(JConfig.queryConfiguration("debug.uber", "false").equals("true")) {
        if(dfa2 != null) {
          if(dfa2.length == 0) {
            ErrorManagement.logVerboseDebug("Length is still zero!");
          }
          for(j=0; j<dfa2.length; j++) {
            ErrorManagement.logVerboseDebug("Flavah " + j + " == " + dfa2[j].getHumanPresentableName());
            ErrorManagement.logVerboseDebug("Flavah/mime " + j + " == " + dfa2[j].getMimeType());
          }
        } else {
          ErrorManagement.logVerboseDebug("Flavahs supported: none!\n");
        }
      }
    }

    if(JConfig.queryConfiguration("debug.uber", "false").equals("true") && JConfig.debugging) dumpFlavorsOld(t);

    dtf = testAllFlavors(t);
    if(dtf != null) {
      ErrorManagement.logVerboseDebug("Accepting!");
      acceptDrop(dtde);

      dropData = getTransferData(t);
      dtde.dropComplete(true);
      dtde.getDropTargetContext().dropComplete(true);
      if(dropData != null) {
        if(handler != null) {
          handler.receiveDropString(dropData);
        }
      }
    } else {
      ErrorManagement.logVerboseDebug("Rejecting!");
      dtde.rejectDrop();
      handler.receiveDropString(dropData);
    }
  }
}