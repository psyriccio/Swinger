# Swinger
### Swing UI enhancements and helpers library


---

### Some examples

###### Processing of components tree

```java
public static void doWithEntireComponentTree(Component component, Consumer<Component> consumer)
```

```java
Swinger.doWithEntireComponentTree(myFrame, (c) -> c.setBackground(Color.BLUE) );
```
This code sets blue background color for myFrame component and for all of myFrame childs, and for child childs, and so on...

###### Font loading and assigning to components tree

```java
public static void loadFontAsMain(File fontFile, Component... components);
```
```java
Swinger.loadFontAsMain(new File("path/to/my/cool/font.ttf"), myFrame, myPanel, myButton);
```
Load font fron file and apply to myFrame-based component tree, then to myPanel-based, then to myButton

##### [javadoc](https://psyriccio.github.io/Swinger/)
