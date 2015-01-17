package net.wetheinter.webcomponent.client.io;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Async Counterpart: {@link GwtcIOAsync}
 *
 * @author James X Nelson
 *
 */
@RemoteServiceRelativePath("../gwtc")
public interface GwtcIO extends RemoteService {

  String compile(String target, String arguments);

}
