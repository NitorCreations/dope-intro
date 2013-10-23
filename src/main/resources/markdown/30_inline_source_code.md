# Inline source code #

 * [pygments](http://pygments.org) supports lots of languages - here are some samples 
 * Python

```python
print highlight(code, PythonLexer(), HtmlFormatter())
```

 * Haskell

```haskell
let (a:b:c:[]) = "xyz" in a
```

 * D

```d
import std.stdio;

void main() {
    ulong lines = 0;
```

 * and lots more at http://pygments.org/languages/
