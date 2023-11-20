declare var jQuery: JQueryStatic;
declare var $: JQueryStatic;

function insertBody(film: any) {
	return `<tr >
				<td width="66%" align="center" style="text-align: left;">
					<div style="height: 100%; padding: 1em; display: flex; flex-direction: column;" class="ui-accordion-content ui-corner-all ui-helper-reset ui-widget-content ui-accordion-content-active">
						<span style="font-weight: bold; ">
							<a class="ui-button ui-widget ui-corner-all" href="https://www.imdb.com/title/tt${film.mainImdbid}" target ="_blank">
								${film.mainOriginalTitle}
							</a>
							&nbsp;&nbsp;<span data-color="${film.mainRating}">${film.mainRating}</span> ${film.mainVotes}</span>
						<br/>
						<span><b>${film.mainKind}&nbsp;${film.name} | ${film.originalName}</b></span>
						<span><b>Ratio:</b> ${film.mainAspectRatio}, <b>Year:</b> ${film.mainYear}${film.mainCountries}</span>
						<span><b>${film.runtimeHM}</b>, <i>${film.mainGenres}</i>, ${film.size} ${film.fileCount}</span>
						<span><b>Director:</b> ${film.mainDirectors}</span>
						<span><b>Writer:</b> ${film.mainWriters}</span>
						<span><b>Actors:</b> ${film.mainStars}</span>
						<br>
						<div class="tabs">
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

var dc: number;
var first;
var second;
var th: {addClass: (arg0: any) => void;};

$(document).ready(function () {
	var text = "";
	for (let film of jsonByRating) {
		text += insertBody(film)
	}
	$("#status2")[0].innerHTML = `${$("#status2")[0].innerHTML} \n <div id="tabs-${2}"><table><tbody>${text}</tbody></table></div>`;

	var text = "";
	for (let film of jsonByDate) {
		text += insertBody(film)
	}
	$("#status2")[0].innerHTML = `${$("#status2")[0].innerHTML} \n <div id="tabs-${3}"><table><tbody>${text}</tbody></table></div>`;

	$(function () {
		$(".accordion").accordion({
			heightStyle: "content"
		});
		$("#tabs-1").tabs();
	});

	$(function () {
		$(".tabs").tabs();
	});

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
});