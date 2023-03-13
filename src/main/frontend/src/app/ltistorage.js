"use strict";
class LtiStorage {
    async initToolLogin(platformOidcLoginUrl, oidcLoginData, launchFrame) {
        return this.setStateAndNonce(platformOidcLoginUrl, oidcLoginData, launchFrame)
            .then(this.doLoginInitiationRedirect);
    }
    async setStateAndNonce(platformOidcLoginUrl, oidcLoginData, launchFrame) {
        let launchWindow = launchFrame || window;
        let state = LtiPostMessage.secureRandom();
        let nonce = LtiPostMessage.secureRandom();
        return new Promise((resolve, reject) => {
            let params = new URLSearchParams(window.location.search);
            return resolve(params.has('lti_storage_target'));
        })
            .then(async (hasPlatformStorage) => {
            if (hasPlatformStorage) {
                let platformStorage = this.ltiPostMessage(new URL(platformOidcLoginUrl.origin), launchWindow);
                return platformStorage.putData(LtiStorage.cookiePrefix + '_state_' + state, state)
                    .then(() => platformStorage.putData(LtiStorage.cookiePrefix + '_nonce_' + nonce, nonce));
            }
            return Promise.reject();
        })
            .catch((err) => {
            err && console.log(err);
            return this.setStateAndNonceCookies(state, nonce);
        })
            .then((hasState) => {
            let data = {
                ...oidcLoginData,
                state: state,
                nonce: nonce,
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
        let platformStorage = this.ltiPostMessage(platformOrigin, launchFrame);
        console.log("platformStorage var", platformStorage);
        console.log("platformStorage cookiePrefix", LtiStorage.cookiePrefix);
        console.log("platformStorage state", state);
        return platformStorage.getData(LtiStorage.cookiePrefix + '_state_' + state)
            .then((value) => {
            if (!value || state !== value) {
                console.log("platformStorage value 2", value);
                console.log("platformStorage state 2", state);
                return Promise.reject();
            }
            return platformStorage.getData(LtiStorage.cookiePrefix + '_nonce_' + nonce);
        })
            .then((value) => {
            if (!value || nonce !== value) {
                console.log("platformStorage value 2", value);
                console.log("platformStorage nonce 2", nonce);
                return Promise.reject();
            }
            return true;
        })
            .catch(() => { return false; });
    }
    ltiPostMessage(targetOrigin, launchFrame) {
        console.log("ltiPostMessage - targetOrigin", targetOrigin);
        console.log("ltiPostMessage - launchFrame", launchFrame);
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
        this._targetOrigin = targetOrigin;
        this._launchFrame = launchFrame || window;
    }
    static secureRandom(length) {
        let random = new Uint8Array(length || 63);
        crypto.getRandomValues(random);
        return btoa(String.fromCharCode(...random)).replace(/\//g, '_').replace(/\+/g, '-');
    }
    async sendPostMessage(data, targetWindow, originOverride, targetFrameName) {
        return new Promise((resolve, reject) => {
            // let log = new LtiPostMessageLog(this.#debug);
            let timeout;
            let targetOrigin = originOverride || this._targetOrigin.origin;
            data.message_id = 'message-' + LtiPostMessage.secureRandom(15);
            let targetFrame;
            try {
                targetFrame = this.getTargetFrame(targetWindow, targetFrameName);
            }
            catch (e) {
                //log.error({message: 'Failed to access target frame with name: [' + targetFrameName + '] falling back to use target window - Error: [' + e + ']'});
                console.error("sendPostMessage - error", e);
                targetFrameName = undefined;
                targetFrame = targetWindow;
            }
            const messageHandler = (event) => {
                if (event.data.message_id !== data.message_id) {
                    console.log("messageHandler - data.message_id", data.message_id);
                    console.log("messageHandler - event", event);
                    console.log("messageHandler - data.message_id", data.message_id);
                    //log.error({message: 'Ignoring message, invalid message_id: [' + event.data.message_id + '] expected: [' + data.message_id + ']'});
                    return;
                }
                /*
                log.response(event);
                if (targetOrigin !== '*' && event.origin !== targetOrigin) {
                    log.error({message: 'Ignoring message, invalid origin: ' + event.origin});
                    return log.print();
                }
                if (event.data.subject !== data.subject + '.response') {
                    log.error({message: 'Ignoring message, invalid subject: [' + event.data.subject + '] expected: [' + data.subject + '.response]'});
                    return log.print();
                }
                */
                window.removeEventListener('message', messageHandler);
                clearTimeout(timeout);
                if (event.data.error) {
                    // log.error(event.data.error);
                    // log.print();
                    return reject(event.data.error);
                }
                //log.print();
                resolve(event.data);
            };
            window.addEventListener('message', messageHandler);
            //log.request(targetFrameName, data, targetOrigin);
            targetFrame.postMessage(data, targetOrigin);
            timeout = setTimeout(() => {
                window.removeEventListener('message', messageHandler);
                let timeout_error = {
                    code: 'timeout',
                    message: 'No response received after 1000ms'
                };
                // log.error(timeout_error);
                // log.print();
                reject(timeout_error);
            }, 1000);
        });
    }
    ;
    async sendPostMessageIfCapable(data) {
        // Call capability service
        return Promise.any([
            this.sendPostMessage({ subject: 'lti.capabilities' }, this.getTargetWindow(), '*'),
            // Send new and old capabilities messages for support with pre-release subjects
            this.sendPostMessage({ subject: 'org.imsglobal.lti.capabilities' }, this.getTargetWindow(), '*')
        ])
            .then((capabilities) => {
            if (typeof capabilities.supported_messages == 'undefined') {
                return Promise.reject({
                    code: 'not_found',
                    message: 'No capabilities'
                });
            }
            console.log("sendPostMessageIfCapable - data", data);
            console.log("sendPostMessageIfCapable - capabilities", capabilities);
            console.log("sendPostMessageIfCapable - target window", this.getTargetWindow());
            for (let i = 0; i < capabilities.supported_messages.length; i++) {
                if (![data.subject, 'org.imsglobal.' + data.subject].includes(capabilities.supported_messages[i].subject)) {
                    continue;
                }
                // Use subject specified in capabilities for backwards compatibility
                data.subject = capabilities.supported_messages[i].subject;
                return this.sendPostMessage(data, this.getTargetWindow(), undefined, capabilities.supported_messages[i].frame);
            }
            return Promise.reject({
                code: 'not_found',
                message: 'Capabilities not found'
            });
        });
    }
    ;
    async putData(key, value) {
        return this.sendPostMessageIfCapable({
            subject: 'lti.put_data',
            key: key,
            value: value
        })
            .then((response) => {
            return true;
        });
    }
    ;
    async getData(key) {
        return this.sendPostMessageIfCapable({
            subject: 'lti.get_data',
            key: key
        })
            .then((response) => {
            console.log("getData key", key);
            console.log("getData response", response);
            return response.value;
        });
    }
    ;
    getTargetWindow() {
        return this._launchFrame.opener || this._launchFrame.parent;
    }
    ;
    getTargetFrame(targetWindow, frameName) {
        if (frameName && targetWindow.frames[frameName]) {
            return targetWindow.frames[frameName];
        }
        return targetWindow;
    }
}

export default LtiStorage;
