package com.shroman.secureraid.client;

import com.shroman.secureraid.common.Response;

public interface PushResponseInterface {
	void push(Response response, int serverId);
}
