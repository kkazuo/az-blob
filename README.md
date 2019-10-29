# az-blob

A Clojure library designed to access Azure Blob Storage API without Azure SDK.

Azure SDK has huge dependencies.

You can avoid dependency conflict with this library.

## Usage

```
(require '[az-blob.core :as az])
```

First of all, make account map from connection string.

```
(def ac (conn-str->map "DefaultEndpointsProtocol=https;...=core.windows.net"))
```

List containers.

```
(list-containers ac)
```

List blobs in a container.

```
(list-blobs ac "example")
```

Get Blob in a container.

```
(get-blob ac "example" "test.txt")
```

Put Blob to a container.

```
(put-blob "hello" ac "example" "hello.txt")
```

## License

Copyright © 2019 Koga Kazuo

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
