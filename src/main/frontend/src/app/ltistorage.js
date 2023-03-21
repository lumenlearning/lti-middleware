
class LtiStorage {
    // We never use the initToolLogin method from this file but keeping it here for now
    async initToolLogin(platformOidcLoginUrl, oidcLoginData, launchFrame) {
        return this.setStateAndNonce(platformOidcLoginUrl, oidcLoginData, launchFrame)
            .then(this.doLoginInitiationRedirect);
    }
    async setStateAndNonce(platformOidcLoginUrl, oidcLoginData, launchFrame) {
        let launchWindow = launchFrame || window;
        return new Promise((resolve, reject) => {
            let params = new URLSearchParams(window.location.search);
            return resolve(params.has('lti_storage_target'));
        })
            .then(async (hasPlatformStorage) => {
                if (hasPlatformStorage) {
                    let platformStorage = this.ltiPostMessage(new URL(platformOidcLoginUrl.origin), launchWindow);
                    return platformStorage.putData(LtiStorage.cookiePrefix + '_state_' + oidcLoginData.state, oidcLoginData.state)
                        .then(() => platformStorage.putData(LtiStorage.cookiePrefix + '_nonce_' + oidcLoginData.nonce, oidcLoginData.nonce));
                }
                return Promise.reject();
            })
            .catch((err) => {
                err && console.log(err);
                return this.setStateAndNonceCookies(oidcLoginData.state, oidcLoginData.nonce);
            })
            .then((hasState) => {
                let data = {
                    ...oidcLoginData,
                    scope: 'openid',
                    response_type: 'id_token',
                    response_mode: 'form_post',
                    prompt: 'none', // Don't prompt user on redirect.
                };
                return {
                    url: platformOidcLoginUrl,
                    params: data,
                    target: hasState ? '_self' : '_blank',
                };
            });
    }
    doLoginInitiationRedirect(formData) {
        let form = document.createElement("form");
        for (let key in formData.params) {
            let element = document.createElement("input");
            element.type = 'hidden';
            element.value = formData.params[key];
            element.name = key;
            form.appendChild(element);
        }
        ;
        form.method = "POST";
        form.action = formData.url.toString();
        document.body.appendChild(form);
        form.submit();
    }
    async validateStateAndNonce(state, nonce, platformOrigin, launchFrame) {
        // Check cookie first
        if (document.cookie.split('; ').includes(LtiStorage.cookiePrefix + '_state_' + state + '=' + state)
            && document.cookie.split('; ').includes(LtiStorage.cookiePrefix + '_nonce_' + nonce + '=' + nonce)) {
            // Found state in cookie, return true
            return Promise.resolve(true);
        }
        console.log("state: " + state);
        let platformStorage = this.ltiPostMessage(platformOrigin, launchFrame);
        return platformStorage.getData(LtiStorage.cookiePrefix + '_state_' + state)
            .then((value) => {
                console.log("state value", value);
                if (!value || state !== value) {
                    console.log("rejected point a");
                    return Promise.reject();
                }
                return platformStorage.getData(LtiStorage.cookiePrefix + '_nonce_' + nonce);
            })
            .then((value) => {
                if (!value || nonce !== value) {
                    console.log("rejected point b");
                    return Promise.reject();
                }
                console.log("i succeeded");
                return true;
            })
            .catch((e) =>
            {
                console.log("error: ", e);
                return false;
            });
    }
    ltiPostMessage(targetOrigin, launchFrame) {
        return new LtiPostMessage(targetOrigin, launchFrame);
    }
    setStateAndNonceCookies(state, nonce) {
        document.cookie = LtiStorage.cookiePrefix + '_state_' + state + '=' + state + '; path=/; samesite=none; secure; expires=' + (new Date(Date.now() + 300 * 1000)).toUTCString();
        document.cookie = LtiStorage.cookiePrefix + '_nonce_' + nonce + '=' + nonce + '; path=/; samesite=none; secure; expires=' + (new Date(Date.now() + 300 * 1000)).toUTCString();
        return document.cookie.split('; ').includes(LtiStorage.cookiePrefix + '_state_' + state + '=' + state)
            && document.cookie.split('; ').includes(LtiStorage.cookiePrefix + '_nonce_' + nonce + '=' + nonce);
    }
}
LtiStorage.cookiePrefix = 'lti';
class LtiPostMessage {
    constructor(targetOrigin, launchFrame) {
        this._targetOrigin = new URL(targetOrigin);
        this._launchFrame = launchFrame || window;
    }
    static secureRandom(length) {
        let random = new Uint8Array(length || 63);
        crypto.getRandomValues(random);
        return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
    }
    async sendPostMessage(data, targetWindow, originOverride, targetFrameName) {
        return new Promise((resolve, reject) => {
            let timeout;
            let targetOrigin = originOverride || this._targetOrigin.origin;
            let targetFrame;
            try {
                targetFrame = this.getTargetFrame(targetWindow, targetFrameName);
            }
            catch (e) {
                targetFrameName = undefined;
                targetFrame = targetWindow;
            }
            const messageHandler = (event) => {
                if (event.data.message_id !== data.message_id) {
                    return;
                }
                window.removeEventListener('message', messageHandler);
                clearTimeout(timeout);
                if (event.data.error) {
                    return reject(event.data.error);
                }
                resolve(event.data);
            };
            window.addEventListener('message', messageHandler);
            targetFrame.postMessage(data, targetOrigin);
            timeout = setTimeout(() => {
                console.log("timeout", data);
                window.removeEventListener('message', messageHandler);
                let timeout_error = {
                    code: 'timeout',
                    message: 'No response received after 1000ms'
                };
                reject(timeout_error);
            }, 1000);
        });
    }
    ;
    async sendPostMessageIfCapable(data) {
        function secureRandom(length) {
          let random = new Uint8Array(length||63);
          crypto.getRandomValues(random);
          return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
        }
        // Call capability service
        return Promise.any([
            this.sendPostMessage(
               {
                    subject: 'lti.capabilities',
                    message_id: 'message-' + secureRandom(15)
                },
                this.getTargetWindow(),
                '*'
            ),
            // Send new and old capabilities messages for support with pre-release subjects
            this.sendPostMessage(
                {
                    subject: 'org.imsglobal.lti.capabilities',
                    message_id: 'message-' + secureRandom(15)
                },
                this.getTargetWindow(),
                '*'
            )
        ])
            .then((capabilities) => {
                console.log("got capabilities", capabilities);
                if (typeof capabilities.supported_messages == 'undefined') {
                    return Promise.reject({
                        code: 'not_found',
                        message: 'No capabilities'
                    });
                }

                for (let i = 0; i < capabilities.supported_messages.length; i++) {
                    if (![data.subject, 'org.imsglobal.' + data.subject].includes(capabilities.supported_messages[i].subject)) {
                        continue;
                    }
                    // Use subject specified in capabilities for backwards compatibility
                    data.subject = capabilities.supported_messages[i].subject;
                    // Setting the override to "*"
                    console.log(capabilities.supported_messages[i]);
                    console.log(capabilities.supported_messages[i].frame);
                    return this.sendPostMessage(
                        data,
                        this.getTargetWindow(),
                        "*",
                        capabilities.supported_messages[i].frame
                    );
                }
                return Promise.reject({
                    code: 'not_found',
                    message: 'Capabilities not found'
                });
            });
    };
    async putData(key, value) {
        function secureRandom(length) {
            let random = new Uint8Array(length||63);
            crypto.getRandomValues(random);
            return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
        };
        console.log("put data");
        return {
          subject: "org.imsglobal.lti.put_data",
          key: key,
          value: value,
          message_id: 'message-' + secureRandom(15)
        };
    };
    async getData(key) {
        function secureRandom(length) {
            let random = new Uint8Array(length||63);
            crypto.getRandomValues(random);
            return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
        };
        return this.sendPostMessageIfCapable({
            subject: 'lti.get_data',
            key: key,
            message_id: 'message-' + secureRandom(15)
        })
            .then((response) => {
                console.log("getData response", response);
                return response.value;
            });
    };

    getTargetWindow() {
        return this._launchFrame.opener || this._launchFrame.parent;
    };

    getTargetFrame(targetWindow, frameName) {
        if (frameName && targetWindow.frames[frameName]) {
            return targetWindow.frames[frameName];
        }
        return targetWindow;
    }
}

export default LtiStorage;
