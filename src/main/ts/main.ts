function center(el: HTMLElement) {
    function documentOffsetTop (el: HTMLElement) {
        let top = el.offsetTop;
        let parent = el.offsetParent;
        if (parent && parent instanceof HTMLElement) {
            top += documentOffsetTop(parent);
        }
        return top;
    }

    let top = documentOffsetTop(el) - (window.innerHeight / 2);
    window.scrollTo( 0, top );
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

highlight(fromUrl());
window.addEventListener("hashchange", function () { highlight(fromHash()) }, false);

let comments = document.getElementsByClassName("field-editable");
for (let i = 0; i < comments.length; i++) {
    let comment = comments[i];
    comment.addEventListener("change", e => {
        let target = <HTMLInputElement>(e.target);
        let field = target.dataset.field || "";
        let address = parseInt(target.dataset.address || "-1");
        let value = (target).value;
        alert(field + "/" + address + "=" + value);
        return false;
    });
}