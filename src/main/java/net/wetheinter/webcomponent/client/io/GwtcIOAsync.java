package net.wetheinter.webcomponent.client.io;

import com.google.gwt.user.client.rpc.AsyncCallback;

public interface GwtcIOAsync {

  void compile(String target, String arguments, AsyncCallback<String> callback);

}
