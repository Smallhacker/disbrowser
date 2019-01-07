function center(el) {
    function documentOffsetTop (el) {
        return el.offsetTop + ( el.offsetParent ? documentOffsetTop(el.offsetParent) : 0 );
    }

    var top = documentOffsetTop(el) - ( window.innerHeight / 2 );
    window.scrollTo( 0, top );
}

function highlight(el) {
    if (el) {
        //el.scrollIntoView();
        center(el);
        var cl = el.classList;
        var activeClass = "line-active";
        cl.add(activeClass);
        setTimeout(cl.remove.bind(cl, activeClass), 100);
    }
}

function fromUrlPart(regex, input) {
    var match = regex.exec(input);

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



