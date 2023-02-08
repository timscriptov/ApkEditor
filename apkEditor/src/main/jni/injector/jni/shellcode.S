.global _dlopen_addr_s
.global _dlopen_param1_s
.global _dlopen_param2_s

.global _dlsym_addr_s
.global _dlsym_param2_s

.global _dlclose_addr_s

.global _inject_start_s
.global _inject_end_s

.global _hook_entry_addr_s

.global _inject_function_param_s

.global _saved_cpsr_s
.global _saved_r0_pc_s

.data

_hook_entry_addr_s:
.word 0x01

_inject_start_s:
	@ debug loop
3:
	@sub r1, r1, #0
	@B 3b

	@dlopen
	ldr r1, _dlopen_param2_s
	ldr r0, _dlopen_param1_s
	ldr r3, _dlopen_addr_s
	blx r3
	subs r4, r0, #0
	beq	2f

	@dlsym
	ldr r1, _dlsym_param2_s
	ldr r3, _dlsym_addr_s
	blx r3
	subs r3, r0, #0
	@beq 1f

	@call our function
	ldr r0, _inject_function_param_s
	@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@
	@ldr r5, =_hook_entry_addr_s;
	@str r3, [r5]
	@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@	
	blx r3  @function's address is stored in r3
	subs r0, r0, #0
	beq 2f

2:
	@restore context
	ldr r1, _saved_cpsr_s
	msr cpsr_cf, r1
	ldr sp, _saved_r0_pc_s
	ldmfd sp, {r0-pc}

_dlopen_addr_s:
.word 0x11111111

_dlopen_param1_s:
.word 0x11111111

_dlopen_param2_s:
.word 0x2

_dlsym_addr_s:
.word 0x11111111

_dlsym_param2_s:
.word 0x11111111

_dlclose_addr_s:
.word 0x11111111

_inject_function_param_s:
.word 0x11111111

_saved_cpsr_s:
.word 0x11111111

_saved_r0_pc_s:
.word 0x11111111

_inject_end_s:

.space 0x400, 0

.end