declare var jQuery: JQueryStatic;
declare var $: JQueryStatic;

function insertAll(film: any) {
	return `
				<div class="hist-outer-cell" align="center">
					<a href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank">
						<img style="padding-top: 1em;" src="${film.mainCoverUrl}" alt="${film.mainOriginalTitle}" width="300"/>
					</a>
					<div class="hist-text-cell">
						<span>${film.rank}<span style="font-size: x-small;">/${historyCount}</span>&nbsp;
							<span style="font-weight: bold; border:0px; text-wrap: balance;">
								<a class="ui-button ui-widget ui-corner-all" href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank">
									${film.mainOriginalTitle}
								</a>
							</span>
						</span>
						<br/>
						<span style="text-wrap: balance;">${film.mainKind}&nbsp;<span data-color="${film.mainRating}">${film.mainRating}</span>&nbsp;${film.mainVotes}&nbsp;<small style="font-weight: lighter; font-size: small; font-family: monospace;">${film.mainImdbid}</small></span>
						<span style="text-wrap: balance;"><b>Year:</b> ${film.mainYear}${film.mainCountries}</span>
						<span style="text-wrap: balance;"><b>${film.runtimeHM}</b>, <i>${film.mainGenres}</i></span>
					</div>
				</div>`;
}

function insertBody(film: any) {
	return `<tr >
				<td width="66%" align="center" style="text-align: left;">
					<div style="height: 100%; padding: 1em; display: flex; flex-direction: column;" class="ui-accordion-content ui-corner-all ui-helper-reset ui-widget-content ui-accordion-content-active">
						<span style="font-weight: bold; ">
							<a class="ui-button ui-widget ui-corner-all" href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank">
								${film.mainOriginalTitle}
							</a>
							&nbsp;&nbsp;<span data-color="${film.mainRating}">${film.mainRating}</span> ${film.mainVotes}&nbsp;<small style="font-weight: lighter; font-size: small; font-family: monospace;">${film.mainImdbid}</small></span>
						<br/>
						<span><b>${film.mainKind}&nbsp;${film.name} | ${film.originalName}</b></span>
						<span><i>${film.mainGenres}</i></span>
						<table>
							<tr>
								<td rowspan="2"><b>Ratio:</b>&nbsp;${film.mainAspectRatio},&nbsp;<b>Year:</b>&nbsp;${film.mainYear}${film.mainCountries},&nbsp;</td>
								<td rowspan="2" style="font-size: x-small;padding: 0px;margin: 0px;">${film.mainLanguages}&nbsp;</td>
								<td style="font-size: x-small;padding: 0px;margin: 0px;"><b>Audio:</b>&nbsp;${film.audio}</td>
							</tr>
							<tr>
								<td style="font-size: x-small; width: 40em;padding: 0px;margin: 0px;"><b>Sub&nbsp;Title:</b>&nbsp;${film.subTitles}</td>
							</tr>
						</table>
						<span><b>Duration: </b>${film.runtimeHM}, <b>Resolution: </b>${film.resolutionDescription === null ? film.width + 'x' + film.heigth : film.resolutionDescription}, <b>Codec: </b>${film.codecDescription}, <b>Size: </b>${film.size} ${film.fileCount === null ? '' : ', <b>Count: </b>' + film.fileCount}</span>
			<span><b>Director: </b> ${film.mainDirectors}</span >
				<span><b>Writer: </b> ${film.mainWriters}</span >
					<span><b>Actors: </b> ${film.mainStars}</span >
						<br>
						<div class="inner-tabs">
							<ul>
								<li><a href="#tabs-1">Plot</a></li>
								<li><a href="#tabs-2">Synopsis</a></li>
									</ul>
							<div id="tabs-1">
								${film.plotPlot}
							</div>
							<div id="tabs-2">
								${film.plotSynopsis}
							</div>
						</div>
					</div>
				</td>
				<td width="33%" align="center">
					<a href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank"><img src="${film.mainCoverUrl}" alt="${film.name}" width="300"/></a>
				</td>
			</tr>`;
}

var mc = {
	'0-4.9': 'red',
	'5.0-5.9': 'orangered',
	'6.0-7.4': 'blue',
	'7.5-7.9': 'green',
	'8.0-10.0': 'gold'
};

function between(x: number, min: number, max: number) {
	return x >= min && x <= max;
}

function screenshotPreview() {
	var xOffset = 10;
	var yOffset = 30;

	$("a.screenshot").hover(function (e) {
		this.t = this.title;
		this.title = "";
		var c = (this.t !== "") ? "<br/>" + this.t : "";
		$("body").append("<p id='screenshot'><img src='" + this.rel + "' alt='url preview' />" + c + "</p>");
		$("#screenshot")
			.css("top", (e.pageY - xOffset) + "px")
			.css("left", (e.pageX + yOffset) + "px")
			.fadeIn("fast");
	},
		function () {
			this.title = this.t;
			$("#screenshot").remove();
		});
	$("a.screenshot").mousemove(function (e) {
		$("#screenshot")
			.css("top", (e.pageY - xOffset) + "px")
			.css("left", (e.pageX + yOffset) + "px");
	});
};

var dc: number;
var first;
var second;
var th: {addClass: (arg0: any) => void;};

$(document).ready(function () {

	const titles = Object.keys(jsonListAll[0])
		.map((key) => `<th>${key}</th>`)
		.join("");
	const rows = jsonListAll.map((obj) =>
		`<tr>${Object.values(obj)
			.map((val) => `<td>${val}</td>`)
			.join("")}</tr>`
	).join("");

	$("#table_and_search")[0].innerHTML = `
										<thead>
											<tr>
												${titles}
											</tr>
										</thead>
										<tbody>
											${rows}
										</tbody>`;

	$('#table_and_search').DataTable({
		info: false,
		ordering: true,
		paging: false,
		colReorder: {
			order: [6, 0, 7, 4, 5, 3, 2, 1, 9, 10, 11, 8]
		},
		order: {
			name: 'rank',
			dir: 'asc'
		},
		columnDefs: [
			{
				target: 2,
				visible: false,
				searchable: false
			}, {
				target: 5,
				visible: false,
				searchable: false
			}, {
				target: 9,
				visible: false,
				searchable: false
			}
		],
		columns: [
			{
				data: "mainRating",
				title: "Rating",
				render: function (data, type, row, meta) {
					if (type === 'display') {
						return "<span data-color='" + data + "'>" + data + "</span>";
					}
					return data;
				}
			},
			{
				data: "runtimeHM",
				title: "Duration"
			},
			{
				data: "mainCoverUrl"
			},
			{
				data: "mainKind",
				title: "Kind"
			},
			{
				data: "mainYear",
				title: "Year"
			},
			{
				data: "mainVotes",
				title: "Votes"
			},
			{
				data: "rank",
				title: "Rank"
			},
			{
				data: "mainOriginalTitle",
				title: "Title",
				render: function (data, type, row, meta) {
					if (type === 'display') {
						return "<a class='screenshot' href='https://www.imdb.com/title/tt" + row.mainImdbid + "/' target='_blank' rel='" + row.mainCoverUrl + "'>" + data + "</a>";
					}
					return data;
				}
			},
			{
				data: "mainImdbid",
				title: "IMDB (id)",
				render: function (data, type, row, meta) {
					if (type === 'display') {
						return data + "&nbsp;<small>(" + row.id + ")</small>";
					}
					return data;
				}
			},
			{
				data: "id"
			},
			{
				data: "mainCountries",
				title: "Countries",
				render: function (data, type, row, meta) {
					if (type === 'display') {
						return data.replace(", <b>Country:</b> ", "").replace(", <b>Countries:</b> ", "");
					}
					return data;
				}
			},
			{
				data: "mainGenres",
				title: "Genres"
			}
		]
	});

	screenshotPreview();

	var text = "";
	for (let film of jsonByDate) {
		text += insertBody(film)
	}
	$("#status2")[0].innerHTML = `${$("#status2")[0].innerHTML} \n <div"><table><tbody>${text}</tbody></table></div>`;

	var text = "";
	for (let film of jsonListAll) {
		text += insertAll(film)
	}
	$("#historyData")[0].innerHTML = `${$("#historyData")[0].innerHTML}
		${text}
		</br>
		<div style="left: 50%; transform: translate(-50%, -50%);" class="ui-button ui-widget ui-corner-all" onclick="window.scrollTo({ top: 0, left: 0, behavior: 'smooth' });">&nbsp;&nbsp;Top&nbsp;&nbsp;</div>`;

	$('span').each(function (index: any) {
		th = $(this);
		dc = parseInt($(this).attr('data-color'), 10);
		$.each(mc, function (name: string, value: any) {
			first = parseInt(name.split('-')[0], 10);
			second = parseInt(name.split('-')[1], 10);
			if (between(dc, first, second)) {
				th.addClass(value);
			}
		});
	});

	$(function () {
		$("#tabs").tabs({
			beforeActivate: function (event, ui) {
				if (ui.newTab.index() === 1) {
					$('#table_and_search_wrapper').css('visibility', 'visible');
				} else {
					$('#table_and_search_wrapper').css('visibility', 'collapse');
				}
			}
		});
		$(".inner-tabs").tabs();
		$('#table_and_search_wrapper').css('visibility', 'collapse');
		$("#tabs").addClass('visible');
		$("#tabs").removeClass('hidden');
		$("#loader").css('display', 'none');
	});
});
