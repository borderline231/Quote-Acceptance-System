// Alternative implementation where label completely disappears on focus
// Replace the OutlinedTextField with this if you want the label to disappear:

var isFocused by remember { mutableStateOf(false) }

TextField(
    value = name,
    onValueChange = { name = it },
    placeholder = {
        if (!isFocused || name.isEmpty()) {
            Text("Client Name *")
        }
    },
    modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        },
    singleLine = true,
    colors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent
    )
)

// Or use this simpler version with just placeholder (no label):
TextField(
    value = name,
    onValueChange = { name = it },
    placeholder = { Text("Client Name *") },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true,
    colors = TextFieldDefaults.textFieldColors(
        backgroundColor = Color.Transparent
    )
)