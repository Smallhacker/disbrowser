function center(el: HTMLElement) {
    function documentOffsetTop(el: HTMLElement) {
        let top = el.offsetTop;
        let parent = el.offsetParent;
        if (parent && parent instanceof HTMLElement) {
            top += documentOffsetTop(parent);
        }
        return top;
    }

    let top = documentOffsetTop(el) - (window.innerHeight / 2);
    window.scrollTo(0, top);
}

function highlight(el: HTMLElement | null) {
    if (el) {
        //el.scrollIntoView();
        center(el);
        let cl = el.classList;
        let activeClass = "line-active";
        cl.add(activeClass);
        setTimeout(cl.remove.bind(cl, activeClass), 100);
    }
}

function fromUrlPart(regex: RegExp, input: string) {
    let match = regex.exec(input);

    if (!match) {
        return null;
    }

    return document.getElementById(match[1].toLowerCase());
}

function fromHash() {
    return fromUrlPart(/^#([0-9A-Fa-f]{6})$/, location.hash);
}

function fromPath() {
    return fromUrlPart(/^\/([0-9A-Fa-f]{6})(?:\/.*)?/, location.pathname);
}

function fromUrl() {
    return fromHash() || fromPath();
}

function xhr(url: string, method: string = "GET", body: (string | null) = null) {
    return new Promise((resolve, reject) => {
        let xhr = new XMLHttpRequest();
        xhr.onload = () => {
            if (xhr.status < 400) {
                resolve(xhr);
            } else {
                reject(xhr);
            }
        };
        xhr.onerror = () => reject(xhr);
        xhr.onabort = () => reject(xhr);
        xhr.open(method, url);
        xhr.send(body);
    });
}

highlight(fromUrl());
window.addEventListener("hashchange", function () {
    highlight(fromHash())
}, false);

let editables = document.getElementsByClassName("field-editable");
for (let i = 0; i < editables.length; i++) {
    let editable = editables[i];
    editable.addEventListener("change", e => {
        let target = <HTMLInputElement>(e.target);
        let field = target.dataset.field || "";
        let address = target.dataset.address;
        let game = target.dataset.game;
        let value = (target).value;

        xhr(`/rest/${game}/${address}/${field}`, "POST", value)
            .catch((xhr: XMLHttpRequest) => alert("Error: HTTP " + xhr.status));

        return false;
    });
}

let popupEditables = document.getElementsByClassName("field-editable-popup");
for (let i = 0; i < popupEditables.length; i++) {
    let editable = <HTMLSpanElement>(popupEditables[i]);
    let first = editable.getElementsByClassName("field-editable-popup-icon")[0];
    if (!first) {
        continue;
    }
    first.addEventListener("click", e => {
        let field = editable.dataset.field || "";
        let address = editable.dataset.address;
        let value = editable.dataset.value;
        let game = editable.dataset.game;
        let newValue = prompt("Label for $" + address, value);
        if (newValue === null || newValue == value) {
            return false;
        }

        xhr(`/rest/${game}/${address}/${field}`, "POST", newValue)
            .then(() => location.reload())
            .catch((xhr: XMLHttpRequest) => alert("Error: HTTP " + xhr.status));

        return false;
    });
}

class PersistentProperty<T> {
    private _value: T;
    private readonly key: string;
    private readonly onChange: (value: T) => void;

    constructor(key: string, defaultValue: T, onChange: (value: T) => void) {
        this.key = key;
        this.onChange = onChange;
        this.value = this.parse(key, defaultValue);
    }

    get value(): T {
        return this._value;
    }

    set value(value: T) {
        this._value = value;
        localStorage.setItem(this.key, JSON.stringify(value));
        this.onChange(value);
    }

    private parse(key: string, defaultValue: T): T {
        let value = localStorage.getItem(key);
        if (value === null) {
            return defaultValue;
        }
        let parsedValue = JSON.parse(value);
        if (parsedValue === null) {
            return defaultValue;
        }
        return parsedValue;
    }
}

let darkMode = new PersistentProperty<boolean>(
    "ui.presentation.darkMode",
    false,
    (enable) => {
        if (enable) {
            document.body.classList.add("dark-mode")
        } else {
            document.body.classList.remove("dark-mode")
        }
    }
);

let btnDarkMode = document.getElementById("btn-dark-mode");
if (btnDarkMode) {
    btnDarkMode.addEventListener("click", () => {
        darkMode.value = !darkMode.value;
    })
}