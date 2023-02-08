.class public Lapkeditor/Utils;
.super Ljava/lang/Object;
.source "Utils.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "APKEDITOR"


# direct methods
.method public constructor <init>()V
    .registers 1

    .prologue
    .line 11
    invoke-direct {p0}, Ljava/lang/Object;-><init>()V

    return-void
.end method

.method public static dumpValue(Ljava/lang/Object;)V
    .registers 3
    .param p0, "obj"    # Ljava/lang/Object;

    .prologue
    .line 24
    if-eqz p0, :cond_24

    .line 25
    new-instance v0, Ljava/lang/StringBuilder;

    invoke-direct {v0}, Ljava/lang/StringBuilder;-><init>()V

    const-string v1, "Values of "

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v0

    const-string v1, ":"

    invoke-virtual {v0, v1}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v0

    invoke-virtual {v0}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v0

    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    .line 26
    const/4 v0, 0x1

    const/4 v1, 0x3

    invoke-static {p0, v0, v1}, Lapkeditor/Utils;->getAllValues(Ljava/lang/Object;II)V

    .line 30
    :goto_23
    return-void

    .line 28
    :cond_24
    const-string v0, "null"

    invoke-static {v0}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    goto :goto_23
.end method

.method public static generateImei(I)Ljava/lang/String;
    .registers 11
    .param p0, "offset"    # I

    .prologue
    .line 42
    const-wide v0, 0x2090d9d3b780L

    .line 43
    .local v0, "base":J
    int-to-long v6, p0

    add-long v4, v0, v6

    .line 44
    .local v4, "imei":J
    invoke-static {v4, v5}, Lapkeditor/Utils;->imei_checksum(J)I

    move-result v2

    .line 45
    .local v2, "checksum":I
    const-wide/16 v6, 0xa

    mul-long/2addr v6, v4

    int-to-long v8, v2

    add-long/2addr v6, v8

    invoke-static {v6, v7}, Ljava/lang/String;->valueOf(J)Ljava/lang/String;

    move-result-object v3

    return-object v3
.end method

.method private static getAllValues(Ljava/lang/Object;II)V
    .registers 9
    .param p0, "obj"    # Ljava/lang/Object;
    .param p1, "level"    # I
    .param p2, "maxlevel"    # I

    .prologue
    .line 111
    invoke-virtual {p0}, Ljava/lang/Object;->getClass()Ljava/lang/Class;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/Class;->getDeclaredFields()[Ljava/lang/reflect/Field;

    move-result-object v1

    .line 112
    .local v1, "fields":[Ljava/lang/reflect/Field;
    const/4 v2, 0x0

    .local v2, "i":I
    :goto_9
    array-length v4, v1

    if-ge v2, v4, :cond_6e

    .line 114
    aget-object v4, v1, v2

    invoke-virtual {v4}, Ljava/lang/reflect/Field;->isAccessible()Z

    move-result v4

    if-nez v4, :cond_1a

    .line 115
    aget-object v4, v1, v2

    const/4 v5, 0x1

    invoke-virtual {v4, v5}, Ljava/lang/reflect/Field;->setAccessible(Z)V

    .line 119
    :cond_1a
    :try_start_1a
    aget-object v4, v1, v2

    invoke-virtual {v4, p0}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;

    move-result-object v3

    .line 120
    .local v3, "value":Ljava/lang/Object;
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    invoke-static {p1}, Lapkeditor/Utils;->getPadding(I)Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, "Name: "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    aget-object v5, v1, v2

    invoke-virtual {v5}, Ljava/lang/reflect/Field;->getName()Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ", Value: "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    .line 121
    invoke-static {v3}, Lapkeditor/Utils;->getValueString(Ljava/lang/Object;)Ljava/lang/String;

    move-result-object v5

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    .line 120
    invoke-static {v4}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    .line 122
    if-ge p1, p2, :cond_61

    if-eqz v3, :cond_61

    invoke-static {v3}, Lapkeditor/Utils;->isBasicType(Ljava/lang/Object;)Z

    move-result v4

    if-nez v4, :cond_61

    .line 123
    add-int/lit8 v4, p1, 0x1

    invoke-static {v3, v4, p2}, Lapkeditor/Utils;->getAllValues(Ljava/lang/Object;II)V
    :try_end_61
    .catch Ljava/lang/IllegalArgumentException; {:try_start_1a .. :try_end_61} :catch_64
    .catch Ljava/lang/IllegalAccessException; {:try_start_1a .. :try_end_61} :catch_69

    .line 112
    .end local v3    # "value":Ljava/lang/Object;
    :cond_61
    :goto_61
    add-int/lit8 v2, v2, 0x1

    goto :goto_9

    .line 125
    :catch_64
    move-exception v0

    .line 126
    .local v0, "e":Ljava/lang/IllegalArgumentException;
    invoke-virtual {v0}, Ljava/lang/IllegalArgumentException;->printStackTrace()V

    goto :goto_61

    .line 127
    .end local v0    # "e":Ljava/lang/IllegalArgumentException;
    :catch_69
    move-exception v0

    .line 128
    .local v0, "e":Ljava/lang/IllegalAccessException;
    invoke-virtual {v0}, Ljava/lang/IllegalAccessException;->printStackTrace()V

    goto :goto_61

    .line 131
    .end local v0    # "e":Ljava/lang/IllegalAccessException;
    :cond_6e
    return-void
.end method

.method private static getPadding(I)Ljava/lang/String;
    .registers 6
    .param p0, "level"    # I

    .prologue
    .line 93
    const/16 v3, 0xb

    new-array v0, v3, [Ljava/lang/String;

    const/4 v3, 0x0

    const-string v4, ""

    aput-object v4, v0, v3

    const/4 v3, 0x1

    const-string v4, "  "

    aput-object v4, v0, v3

    const/4 v3, 0x2

    const-string v4, "    "

    aput-object v4, v0, v3

    const/4 v3, 0x3

    const-string v4, "      "

    aput-object v4, v0, v3

    const/4 v3, 0x4

    const-string v4, "        "

    aput-object v4, v0, v3

    const/4 v3, 0x5

    const-string v4, "          "

    aput-object v4, v0, v3

    const/4 v3, 0x6

    const-string v4, "            "

    aput-object v4, v0, v3

    const/4 v3, 0x7

    const-string v4, "              "

    aput-object v4, v0, v3

    const/16 v3, 0x8

    const-string v4, "                "

    aput-object v4, v0, v3

    const/16 v3, 0x9

    const-string v4, "                  "

    aput-object v4, v0, v3

    const/16 v3, 0xa

    const-string v4, "                    "

    aput-object v4, v0, v3

    .line 97
    .local v0, "buffers":[Ljava/lang/String;
    array-length v3, v0

    if-ge p0, v3, :cond_44

    .line 98
    aget-object v3, v0, p0

    .line 106
    :goto_43
    return-object v3

    .line 101
    :cond_44
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 102
    .local v2, "sb":Ljava/lang/StringBuilder;
    const/4 v1, 0x0

    .local v1, "i":I
    :goto_4a
    if-ge v1, p0, :cond_54

    .line 103
    const-string v3, "  "

    invoke-virtual {v2, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 102
    add-int/lit8 v1, v1, 0x1

    goto :goto_4a

    .line 106
    :cond_54
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    goto :goto_43
.end method

.method private static getValueString(Ljava/lang/Object;)Ljava/lang/String;
    .registers 7
    .param p0, "value"    # Ljava/lang/Object;

    .prologue
    .line 70
    instance-of v4, p0, [Ljava/lang/String;

    if-eqz v4, :cond_4c

    .line 71
    check-cast p0, [Ljava/lang/String;

    .end local p0    # "value":Ljava/lang/Object;
    move-object v3, p0

    check-cast v3, [Ljava/lang/String;

    .line 72
    .local v3, "strArray":[Ljava/lang/String;
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 73
    .local v2, "sb":Ljava/lang/StringBuilder;
    const-string v4, "String[]={"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 74
    const/4 v0, 0x0

    .local v0, "i":I
    :goto_14
    array-length v4, v3

    if-ge v0, v4, :cond_42

    .line 75
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, ""

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ":"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    aget-object v5, v3, v0

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ", "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 74
    add-int/lit8 v0, v0, 0x1

    goto :goto_14

    .line 77
    :cond_42
    const-string v4, "}"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 78
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    .line 89
    .end local v0    # "i":I
    .end local v2    # "sb":Ljava/lang/StringBuilder;
    .end local v3    # "strArray":[Ljava/lang/String;
    .restart local p0    # "value":Ljava/lang/Object;
    :goto_4b
    return-object v4

    .line 79
    :cond_4c
    instance-of v4, p0, [Ljava/lang/Integer;

    if-eqz v4, :cond_98

    .line 80
    check-cast p0, [Ljava/lang/Integer;

    .end local p0    # "value":Ljava/lang/Object;
    move-object v1, p0

    check-cast v1, [Ljava/lang/Integer;

    .line 81
    .local v1, "intArray":[Ljava/lang/Integer;
    new-instance v2, Ljava/lang/StringBuilder;

    invoke-direct {v2}, Ljava/lang/StringBuilder;-><init>()V

    .line 82
    .restart local v2    # "sb":Ljava/lang/StringBuilder;
    const-string v4, "Integer[]={"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 83
    const/4 v0, 0x0

    .restart local v0    # "i":I
    :goto_60
    array-length v4, v1

    if-ge v0, v4, :cond_8e

    .line 84
    new-instance v4, Ljava/lang/StringBuilder;

    invoke-direct {v4}, Ljava/lang/StringBuilder;-><init>()V

    const-string v5, ""

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4, v0}, Ljava/lang/StringBuilder;->append(I)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ":"

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    aget-object v5, v1, v0

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/Object;)Ljava/lang/StringBuilder;

    move-result-object v4

    const-string v5, ", "

    invoke-virtual {v4, v5}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v4

    invoke-virtual {v4}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 83
    add-int/lit8 v0, v0, 0x1

    goto :goto_60

    .line 86
    :cond_8e
    const-string v4, "}"

    invoke-virtual {v2, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    .line 87
    invoke-virtual {v2}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v4

    goto :goto_4b

    .line 89
    .end local v0    # "i":I
    .end local v1    # "intArray":[Ljava/lang/Integer;
    .end local v2    # "sb":Ljava/lang/StringBuilder;
    .restart local p0    # "value":Ljava/lang/Object;
    :cond_98
    if-nez p0, :cond_9d

    const-string v4, "null"

    goto :goto_4b

    :cond_9d
    invoke-virtual {p0}, Ljava/lang/Object;->toString()Ljava/lang/String;

    move-result-object v4

    goto :goto_4b
.end method

.method private static imei_checksum(J)I
    .registers 10
    .param p0, "imei"    # J

    .prologue
    .line 49
    const/16 v5, 0xf

    new-array v4, v5, [I

    .line 50
    .local v4, "sum":[I
    const-wide/16 v2, 0xa

    .line 51
    .local v2, "mod":J
    const/4 v1, 0x1

    .local v1, "i":I
    :goto_7
    const/16 v5, 0xe

    if-gt v1, v5, :cond_2f

    .line 52
    rem-long v6, p0, v2

    long-to-int v5, v6

    aput v5, v4, v1

    .line 53
    rem-int/lit8 v5, v1, 0x2

    if-eqz v5, :cond_1a

    .line 54
    aget v5, v4, v1

    mul-int/lit8 v5, v5, 0x2

    aput v5, v4, v1

    .line 56
    :cond_1a
    aget v5, v4, v1

    const/16 v6, 0xa

    if-lt v5, v6, :cond_2b

    .line 57
    aget v5, v4, v1

    rem-int/lit8 v5, v5, 0xa

    aget v6, v4, v1

    div-int/lit8 v6, v6, 0xa

    add-int/2addr v5, v6

    aput v5, v4, v1

    .line 59
    :cond_2b
    div-long/2addr p0, v2

    .line 51
    add-int/lit8 v1, v1, 0x1

    goto :goto_7

    .line 62
    :cond_2f
    const/4 v0, 0x0

    .line 63
    .local v0, "check":I
    const/4 v1, 0x0

    :goto_31
    array-length v5, v4

    if-ge v1, v5, :cond_3a

    .line 64
    aget v5, v4, v1

    add-int/2addr v0, v5

    .line 63
    add-int/lit8 v1, v1, 0x1

    goto :goto_31

    .line 66
    :cond_3a
    mul-int/lit8 v5, v0, 0x9

    rem-int/lit8 v5, v5, 0xa

    return v5
.end method

.method private static isBasicType(Ljava/lang/Object;)Z
    .registers 3
    .param p0, "param"    # Ljava/lang/Object;

    .prologue
    const/4 v0, 0x1

    .line 134
    instance-of v1, p0, Ljava/lang/Integer;

    if-eqz v1, :cond_6

    .line 163
    :cond_5
    :goto_5
    return v0

    .line 136
    :cond_6
    instance-of v1, p0, Ljava/lang/String;

    if-nez v1, :cond_5

    .line 138
    instance-of v1, p0, Ljava/lang/Double;

    if-nez v1, :cond_5

    .line 140
    instance-of v1, p0, Ljava/lang/Float;

    if-nez v1, :cond_5

    .line 142
    instance-of v1, p0, Ljava/lang/Long;

    if-nez v1, :cond_5

    .line 144
    instance-of v1, p0, Ljava/lang/Boolean;

    if-nez v1, :cond_5

    .line 146
    instance-of v1, p0, Ljava/util/Date;

    if-nez v1, :cond_5

    .line 148
    instance-of v1, p0, [Ljava/lang/Integer;

    if-nez v1, :cond_5

    .line 150
    instance-of v1, p0, [Ljava/lang/String;

    if-nez v1, :cond_5

    .line 152
    instance-of v1, p0, [Ljava/lang/Double;

    if-nez v1, :cond_5

    .line 154
    instance-of v1, p0, [Ljava/lang/Float;

    if-nez v1, :cond_5

    .line 156
    instance-of v1, p0, [Ljava/lang/Long;

    if-nez v1, :cond_5

    .line 158
    instance-of v1, p0, [Ljava/lang/Boolean;

    if-nez v1, :cond_5

    .line 160
    instance-of v1, p0, [Ljava/util/Date;

    if-nez v1, :cond_5

    .line 163
    const/4 v0, 0x0

    goto :goto_5
.end method

.method public static log(Ljava/lang/String;)V
    .registers 2
    .param p0, "msg"    # Ljava/lang/String;

    .prologue
    .line 20
    const-string v0, "APKEDITOR"

    invoke-static {v0, p0}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I

    .line 21
    return-void
.end method

.method public static printCallStack()V
    .registers 1

    .prologue
    .line 167
    const/4 v0, 0x0

    invoke-static {v0}, Lapkeditor/Utils;->printCallStack(Ljava/lang/String;)V

    .line 168
    return-void
.end method

.method public static printCallStack(Ljava/lang/String;)V
    .registers 6
    .param p0, "tag"    # Ljava/lang/String;

    .prologue
    .line 171
    if-eqz p0, :cond_4c

    .line 172
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "Stack at "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3, p0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    const-string v4, ": "

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    .line 176
    :goto_1e
    new-instance v0, Ljava/lang/Throwable;

    invoke-direct {v0}, Ljava/lang/Throwable;-><init>()V

    .line 177
    .local v0, "ex":Ljava/lang/Throwable;
    invoke-virtual {v0}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;

    move-result-object v2

    .line 178
    .local v2, "stackElements":[Ljava/lang/StackTraceElement;
    if-eqz v2, :cond_52

    .line 179
    const/4 v1, 0x0

    .local v1, "i":I
    :goto_2a
    array-length v3, v2

    if-ge v1, v3, :cond_52

    .line 180
    new-instance v3, Ljava/lang/StringBuilder;

    invoke-direct {v3}, Ljava/lang/StringBuilder;-><init>()V

    const-string v4, "\t"

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    aget-object v4, v2, v1

    invoke-virtual {v4}, Ljava/lang/StackTraceElement;->toString()Ljava/lang/String;

    move-result-object v4

    invoke-virtual {v3, v4}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v3

    invoke-virtual {v3}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object v3

    invoke-static {v3}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    .line 179
    add-int/lit8 v1, v1, 0x1

    goto :goto_2a

    .line 174
    .end local v0    # "ex":Ljava/lang/Throwable;
    .end local v1    # "i":I
    .end local v2    # "stackElements":[Ljava/lang/StackTraceElement;
    :cond_4c
    const-string v3, "Stack:"

    invoke-static {v3}, Lapkeditor/Utils;->log(Ljava/lang/String;)V

    goto :goto_1e

    .line 183
    .restart local v0    # "ex":Ljava/lang/Throwable;
    .restart local v2    # "stackElements":[Ljava/lang/StackTraceElement;
    :cond_52
    return-void
.end method

.method public static showToast(Landroid/content/Context;Ljava/lang/String;)V
    .registers 3
    .param p0, "ctx"    # Landroid/content/Context;
    .param p1, "msg"    # Ljava/lang/String;

    .prologue
    .line 16
    const/4 v0, 0x1

    invoke-static {p0, p1, v0}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;

    move-result-object v0

    invoke-virtual {v0}, Landroid/widget/Toast;->show()V

    .line 17
    return-void
.end method

.method public static stringAdd1(Ljava/lang/String;)Ljava/lang/String;
    .registers 5
    .param p0, "str"    # Ljava/lang/String;

    .prologue
    .line 33
    if-eqz p0, :cond_33

    const-string v1, ""

    invoke-virtual {p0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v1

    if-nez v1, :cond_33

    .line 34
    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v1

    add-int/lit8 v1, v1, -0x1

    invoke-virtual {p0, v1}, Ljava/lang/String;->charAt(I)C

    move-result v0

    .line 35
    .local v0, "c":C
    add-int/lit8 v1, v0, 0x1

    int-to-char v0, v1

    .line 36
    new-instance v1, Ljava/lang/StringBuilder;

    invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V

    const/4 v2, 0x0

    invoke-virtual {p0}, Ljava/lang/String;->length()I

    move-result v3

    add-int/lit8 v3, v3, -0x1

    invoke-virtual {p0, v2, v3}, Ljava/lang/String;->substring(II)Ljava/lang/String;

    move-result-object v2

    invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(C)Ljava/lang/StringBuilder;

    move-result-object v1

    invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;

    move-result-object p0

    .line 38
    .end local v0    # "c":C
    .end local p0    # "str":Ljava/lang/String;
    :cond_33
    return-object p0
.end method
