<script>
	import "./layout.css";
	import favicon from "$lib/assets/favicon.svg";
	import { page } from "$app/state";
	import { Button } from "flowbite-svelte";
	import { setContext } from "svelte";
	import {
		Navbar,
		NavBrand,
		NavLi,
		NavUl,
		NavHamburger,
	} from "flowbite-svelte";
	let activeUrl = $derived(page.url.pathname);
	let activeClass =
		"text-white bg-green-700 md:bg-transparent md:text-green-700 md:dark:text-white dark:bg-green-600 md:dark:bg-transparent";
	let nonActiveClass =
		"text-gray-700 hover:bg-gray-100 md:hover:bg-transparent md:border-0 md:hover:text-green-700 dark:text-gray-400 md:dark:hover:text-white dark:hover:bg-gray-700 dark:hover:text-white md:dark:hover:bg-transparent";

	let { children } = $props();
</script>

<svelte:head><link rel="icon" href={favicon} /></svelte:head>
<Navbar classes={{ active: activeClass, nonActive: nonActiveClass }}>
	<NavBrand href="/">
		<img src={favicon} alt="Coverage Tool logo" class="h-12 mr-1" />
		<span
			class="self-center text-xl font-semibold whitespace-nowrap dark:text-white"
			>Reach-Aware Coverage</span
		>
	</NavBrand>
	<NavHamburger />
	<NavUl>
		<NavLi href="/">Home</NavLi>
		<NavLi href="/docs">Docs</NavLi>
	</NavUl>
</Navbar>
{@render children()}
