# Gitlet Design Document

这是针对CS61B: Data Structures, Spring 2021版本的gitlet构建，Java 版本控制系统 Git 的独立实现。Gitlet 支持 Git 的大多数本地功能：添加、提交、签出、日志，以及分支操作，包括 merge 和 rebase

### **Name**: ethanyi9

目前所有本地指令构建均已完成


## Classes and Data Structures

### Class 1: Commit

对于需要**提交**的文件的类，此类使用最多，每次进行处理的时候都会构建commit对象

#### Instance Variables

- `String message`: 保存commit提交的评论
- `Date timestamp`: 提交时间，第一个是 `Date(0)`，根据Date对象进行
- `String directParent`: 这个commit的上一个commit。
- `String otherParent`：若是存在`merge`操作，则会使用此变量，记录`merge [branchName]` 中的`branch commit`为上一个节点
- `HashMap<String,String> blobMap`: 文件内容的hashMap，key为track文件的文件名，value是其对应的blob的hash名

#### Methods
getter和setter方法不做讲解，讲解其中其余的方法以及其他类可用的静态方法

成员方法：
- `getHashName`: 获取commit的sha-1 hash值，sha-1包括的内容是message, timestamp, directParent
- `saveCommit()`: 将对象保存进`join(COMMIT_FOLDER, hashname)`中，文件名为commit的hash名
- `addBlob(String fileName, String blobName)`：保存blobMap中键值对
- `removeBlob(String fileName)`：删除blobMap中的指定键值对

静态方法：
- `getHeadCommit()`：用于获取HEAD指针指向的Commit对象
- `getBranchHeadCommit(String branchName, String error_msg)`：用于获取branches文件夹中分支文件指向的Commit对象，error_msg参数是当不存在此branch时需要提供的错误信息
- `getCommit(String hashName)`：通过hashname来获取Commit对象，如果在commit文件夹中不存在此文件则返回null
- `getCommitFromId(String commitId)`：给定一个commitId，返回一个相对应的commit对象，若是没有这个commit对象，则返回null，与getCommit()的区别是支持前缀搜索
- `getSplitCommit(Commit commitA, Commit commitB)`：使用BFS方法查找commitA和commitB的最近的split Commit，不知道什么是split Commit的请翻阅文档

### Class 2 Refs

关于文件指针的类
#### Instance Variables

- `REFS_DIR`: ".gitlet/refs"文件夹
- `HEAD_DIR`: ".gitlet/refs/heads" 文件夹
- `HEAD_CONTENT_PATH`: ".gitlet/HEAD" 文件

...


#### Methods
- `saveBranch(String branchName, String hashName)`: 创建一个文件：路径是`join(HEAD_DIR, branchName)`，向其中写入`hashName`，也就是`commitId`
- `saveHEAD(String branchName, String branchHeadCommitHash)`: 在HEAD文件中写入当前branch的hash值,格式是`branchName + ":" + branchHeadCommitHash`
- `getHeadBranchName()`:从HEAD文件中直接获取当前branch的名字

### Class 3 Blob
用于Blob存储相关的类
#### Instance Variables
- `private String content`：   blob中保存的内容
- `public File filePath`：     blob文件的自身路径
- `private String hashName`：  blob文件名，以hash为值
#### Methods

- `saveBlob()`： 将blob对象保存进 BLOB_FOLDER文件，内容就是blob文件的content

静态方法：
- `getBlobContentFromName(String blobName)`：根据blobName获取到Blob的内容，其中blobName是一个hash值，若是没有这个文件，返回null
- `overWriteFileWithBlob(File file, String content)`：将blob.content中的内容覆盖进file文件中




## Algorithms

### init

`java gitlet.Main init`

创建一个文件夹环境

```
.gitlet (folder)
    |── objects (folder) // 存储commit对象文件
        |-- commits
        |-- blobs
    |── refs (folder)
        |── heads (folder) //指向目前的branch
            |-- master (file)
            |-- other file      //表示其他分支的路径
        |-- HEAD (file)     // 保存HEAD指针的对应hashname
    |-- addstage (folder)       // 暂存区文件夹
    |-- removestage (folder)
```
    
### add

`java gitlet.Main add [file name]`

将指定的文件放入addstage文件夹中，将文件内容创建为blob文件，以内容的hash值作为文件名保存在objects/blobs文件夹中

将当前存在的文件副本添加到暂存stage区域。

暂存已暂存的文件会用新内容覆盖暂存区域中的上一个条目。
暂存区域应位于 .gitlet 中的某个位置。

如果文件的当前工作版本与当前commit中的版本相同，请不要暂存要添加的文件，
如果它已经存在，将其从暂存区域中删除（当文件被更改、添加，然后更改回其原始版本时，可能会发生这种情况）。
```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
        |-- blobs
            |-- <hash>  <----- 加入的file.txt文件内容
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file     
        |-- HEAD (file)     
    |-- addstage (folder)       
        |-- file.txt  <----- 保存blob文件的路径
    |-- removestage (folder)

file.txt  <----- 加入的文件
```

### commit

`java gitlet.Main commit [message]`

将addstage和removestage中的文件一个个进行响应操作，addStage中的进行添加，removeStage中的进行删除

将跟踪文件的快照保存在当前提交和暂存区域中，以便以后可以恢复它们，从而创建新提交。

提交将仅更新它正在跟踪的文件的内容，这些文件在提交时已暂存以进行添加，

在这种情况下，提交现在将包含暂存的文件版本，而不是从其父级获取的版本。提交将保存并开始跟踪任何已暂存以供添加但未被其父级跟踪的文件。

最后，在当前提交中跟踪的文件可能会在新提交中被取消跟踪，因为 rm 命令会暂存以将其删除（如下）。

```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
            | -- <hash> <----- 添加进的commit文件，内容是对应的blob文件名
        |-- blobs
            |-- <hash>  
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file     
        |-- HEAD (file)     
    |-- addstage (folder)       
        |-- file.txt  
    |-- removestage (folder)
file.txt  <----- commit的文件
```


### rm

`java gitlet.Main rm [file name]`

如果文件当前已暂存以进行添加，请取消暂存文件。

如果在当前提交中跟踪了该文件，请暂存该文件以供删除，如果用户尚未从工作目录中删除该文件,则从工作目录删除从此文件。

（如果在commit中跟踪此文件，则可以对它remove，但如果没有跟踪就不能删除）


```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
            | -- <hash> 
        |-- blobs
            |-- <hash>  
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file     
        |-- HEAD (file)     
    |-- addstage (folder)       <----- 若是在addstage中有则删除
    |-- removestage (folder)
        |-- file.txt  <----- 添加
file.txt  <----- 若是在被track状态，则进行删除；若不是在track，就不能删除
```


### log

`java gitlet.Main log`

输出log，内容是从当前HEAD指向的commit以及其所有的parents
格式如下：
```shell
===
commit a0da1ea5a15ab613bf9961fd86f010cf74c7ee48
Date: Thu Nov 9 20:00:05 2017 -0800
A commit message.

===
commit 3e8bf1d794ca2e9ef8a4007275acf3751c7170ff
Date: Thu Nov 9 17:01:33 2017 -0800
Another commit message.

===
commit e881c9575d180a215d1a636545b8fd9abfb1d2bb
Date: Wed Dec 31 16:00:00 1969 -0800
initial commit

```

### global-log

`java gitlet.Main global-log`

输出所有的commit文件


### find

`java gitlet.Main find [commit message]`

输出所有的commit文件

打印出具有给定提交消息的所有提交的 ID，每行一个。

如果有多个这样的提交，它会在单独的行上打印 id。

提交消息是单个操作数;例如 `java gitlet.Main find "initial commit"`

### status

`java gitlet.Main status`

显示当前存在的分支，并用 * 标记当前分支。还显示已暂存以进行添加或删除的文件。格式如下：

```shell
=== Branches ===
*master
other-branch
  
=== Staged Files ===
wug.txt
wug2.txt
  
=== Removed Files ===
goodbye.txt
  
=== Modifications Not Staged For Commit ===
junk.txt (deleted)
wug3.txt (modified)
  
=== Untracked Files ===
random.stuff
```
后面两行算是附加分，若是不想实现可以先用空白代替


### checkout

`java gitlet.Main checkout -- [file name]`

获取 head commit 中存在的文件版本，并将其放入工作目录中，覆盖已经存在的文件版本（如果有）。文件的新版本不会暂存。

`java gitlet.Main checkout [commit id] -- [file name]`

获取提交中具有给定 ID 的文件版本，并将其放入工作目录中，覆盖已经存在的文件版本（如果有）。文件的新版本不会暂存。

`java gitlet.Main checkout [branch name]`

获取给定分支 head 处提交中的所有文件，并将它们放在工作目录中，覆盖已经存在的文件版本（如果存在）。此外，在此命令结束时，给定的分支现在将被视为当前分支 （HEAD）。在当前分支中跟踪但不存在于签出分支中的任何文件都将被删除。除非签出的分支是当前分支，否则暂存区域将被清除


### branch

`java gitlet.Main branch [branch name]`

创建一个具有给定名称的新分支，并将其指向当前头部提交。分支只不过是对提交节点的引用（SHA-1 标识符）的名称。此命令不会立即切换到新创建的分支（就像在真实的 Git 中一样）。在调用 branch 之前，您的代码应该使用名为 “master” 的默认分支运行。

```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
            | -- <hash> 
        |-- blobs
            |-- <hash>  
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file     <----- 指向当前头部提交
        |-- HEAD (file)     
    |-- addstage (folder)       
    |-- removestage (folder)
file.txt  
```

### rm-branch

`java gitlet.Main rm-branch [branch name]`

删除具有给定名称的分支。这仅意味着删除与分支关联的指针(也就是文件）;它并不意味着删除在分支下创建的所有提交，或类似的东西。

```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
            | -- <hash> 
        |-- blobs
            |-- <hash>  
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file     <----- 将此文件删除
        |-- HEAD (file)     
    |-- addstage (folder)       
    |-- removestage (folder)
file.txt  
```


### reset
`java gitlet.Main reset [commit id]`

checkout到给定提交跟踪的所有文件。
删除`[commit id]`中不存在的跟踪文件。
此外，将当前分支的 head 移动到该提交节点`[commit id]`。

`[commit id]`可以缩写为 checkout。暂存区域已清理。
该命令本质上是签出一个任意提交，该提交也会更改当前分支head。

### merge

`java gitlet.Main merge [branch name]`

将提交的`[branch name]`合并至当前分支

情况有以下几种：
1. other：被修改      HEAD：未被修改 --->  working DIR: other, 并且需要被add
2. other：未被修改    HEAD：被修改   --->  working DIR: HEAD
3. other：被修改      HEAD：被修改   ---> 
   1. （一致的修改）    working DIR: HEAD, 相当于什么都不做 
   2. （不一致的修改）  working DIR: Conflict
4. split：不存在      other：不存在    HEAD：被添加   --->  working DIR: HEAD
5. split：不存在      other：被添加    HEAD：不存在   --->  working DIR: other, 并且需要被add
6. other：被删除      HEAD：未被修改   --->  working DIR: 被删除，同时被暂存于removal
7. other：未被修改     HEAD：被删除   --->  working DIR: 被删除

## Persistence
```
.gitlet (folder)
    |── objects (folder) 
        |-- commits
            | -- <hash> 
        |-- blobs
            |-- <hash>  
    |── refs (folder)
        |── heads (folder) 
            |-- master (file)
            |-- other file    
        |-- HEAD (file)     
    |-- addstage (folder)       
    |-- removestage (folder)
file.txt  
```

## Usage

编译
```shell
$ make
```

进行check检测

```shell
$ make check
```

使用相关指令

```
 java gitlet.Main init

 java gitlet.Main add [file name]

 ...

```





